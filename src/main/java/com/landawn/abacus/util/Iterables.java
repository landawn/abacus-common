/*
 * Copyright (c) 2018, Haiyang Li.
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
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
 *
 * <p>
 * When to throw exception? It's designed to avoid throwing any unnecessary
 * exception if the contract defined by method is not broken. For example, if
 * user tries to reverse a {@code null} or empty String. The input String will be
 * returned. But exception will be thrown if try to add an element to a {@code null} Object array or collection.
 *
 * <br />
 * An empty String/Array/Collection/Map/Iterator/Iterable/InputStream/Reader will always be a preferred choice than a {@code null} for the return value of a method.
 *
 * <br />
 * The methods in this class should only read the input {@code Collection/Array/Iterator} parameters, not modify them.
 *
 * <br />
 * The input parameters of most methods in this class should be {@code Iterable/Array}, instead of {@code Collection/Array}.
 * The returned type of most methods in this class should be a type of {@code Optional}, instead of element type of the input {@code Collection/Array}.
 * This class is a utility class, which is designed to extend the methods in {@code CommonUtil/N} class for handling empty input {@code Collection/Array/Iterator/Iterable} parameters or result.
 * </p>
 *
 * @see com.landawn.abacus.util.Comparators
 * @see com.landawn.abacus.util.Fn
 * @see com.landawn.abacus.util.Fn.Fnn
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.CommonUtil
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Index
 * @see com.landawn.abacus.util.Median
 * @see com.landawn.abacus.util.Maps
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.Numbers
 * @see com.landawn.abacus.util.IOUtil
 * @see java.lang.reflect.Array
 * @see java.util.Arrays
 * @see java.util.Collections
 */
public final class Iterables {

    private Iterables() {
        // Utility class.
    }

    // Not sure if it is needed. and also, it is not consistent with the other methods which mostly return Optional.
    /**
     * Returns the first non-null element from the provided two elements.
     * If both are {@code null}, it returns {@code null}.
     *
     * @param a the first element to evaluate
     * @param b the second element to evaluate
     * @return the first non-null element, or {@code null} if both are {@code null}
     * @see N#firstNonNull(Object, Object)
     */
    @Beta
    public static <T> T firstNonNull(final T a, final T b) {
        return a != null ? a : b;
    }

    /**
     * Returns the first non-null element from the provided three elements.
     * If all are {@code null}, it returns {@code null}.
     *
     * @param a the first element to evaluate
     * @param b the second element to evaluate
     * @param c the third element to evaluate
     * @return the first non-null element, or {@code null} if all are {@code null}
     * @see N#firstNonNull(Object, Object, Object)
     */
    @Beta
    public static <T> T firstNonNull(final T a, final T b, final T c) {
        return a != null ? a : (b != null ? b : c);
    }

    /**
     * Returns the first non-null element from the provided array of elements.
     * If the array is {@code null} or empty, or all elements are null, it returns {@code null}.
     *
     * @param a the array of elements to evaluate
     * @return the first non-null element, or {@code null} if the array is {@code null} or empty
     * @see N#firstNonNull(Object[])
     */
    @Beta
    @SafeVarargs
    public static <T> T firstNonNull(final T... a) {
        if (N.isEmpty(a)) {
            return null;
        }

        for (final T e : a) {
            if (e != null) {
                return e;
            }
        }

        return null;
    }

    /**
     * Returns the first non-null element from the provided iterable.
     * If the iterable is {@code null} or empty, or all elements are null, it returns {@code null}.
     *
     * @param c the iterable of elements to evaluate
     * @return the first non-null element, or {@code null} if the iterable is {@code null} or empty
     * @see N#firstNonNull(Iterable)
     * @see Iterables#firstNonNullOrDefault(Iterable, Object)
     */
    @Beta
    public static <T> T firstNonNull(final Iterable<? extends T> c) {
        if (N.isEmpty(c)) {
            return null;
        }

        for (final T e : c) {
            if (e != null) {
                return e;
            }
        }

        return null;
    }

    /**     
     * Returns the first non-null element from the provided iterator.
     * If the iterator is {@code null} or empty, or all elements are null, it returns {@code null}.
     *
     * @param iter the iterator of elements to evaluate
     * @return the first non-null element, or {@code null} if the iterator is {@code null} or empty
     * @see N#firstNonNull(Iterator)
     * @see Iterables#firstNonNullOrDefault(Iterator, Object)
     */
    @Beta
    public static <T> T firstNonNull(final Iterator<? extends T> iter) {
        if (iter == null) {
            return null;
        }

        T e = null;

        while (iter.hasNext()) {
            if ((e = iter.next()) != null) {
                return e;
            }
        }

        return null;
    }

    /**
     * Returns the first non-null element of the given iterable if it is not empty, otherwise returns the specified default value.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the array to check
     * @param defaultValue the default value to return if the iterable is empty
     * @return the first non-null element of the given  iterable if it is not empty, otherwise the specified default value
     * @see #firstNonNullOrDefault(Iterator, Object)
     * @see N#firstNonNull(Iterable)
     */
    @Beta
    public static <T> T firstNonNullOrDefault(final Iterable<? extends T> c, T defaultValue) {
        N.requireNonNull(defaultValue, cs.defaultValue);

        if (CommonUtil.isEmpty(c)) {
            return defaultValue;
        }

        for (final T e : c) {
            if (e != null) {
                return e;
            }
        }

        return defaultValue;
    }

    /**
     * Returns the first non-null element of the given iterator if it is not empty, otherwise returns the specified default value.
     *
     * @param <T> the type of the elements in the iterator
     * @param iter the iterator to check
     * @param defaultValue the default value to return if the iterator is empty
     * @return the first non-null element of the given iterator if it is not empty, otherwise the specified default value
     * @see #firstNonNullOrDefault(Iterable, Object)
     * @see N#firstNonNull(Iterator) 
     */
    @Beta
    public static <T> T firstNonNullOrDefault(final Iterator<? extends T> iter, T defaultValue) {
        N.requireNonNull(defaultValue, cs.defaultValue);

        if (iter == null) {
            return defaultValue;
        }

        while (iter.hasNext()) {
            final T e = iter.next();
            if (e != null) {
                return e;
            }
        }

        return defaultValue;
    }

    /**
     * Returns the last non-null element from the provided two elements.
     * If both are {@code null}, it returns {@code null}.
     *
     * @param a the last element to evaluate
     * @param b the second element to evaluate
     * @return the last non-null element, or {@code null} if both are {@code null}
     * @see N#lastNonNull(Object, Object)
     */
    @Beta
    public static <T> T lastNonNull(final T a, final T b) {
        return b != null ? b : a;
    }

    /**
     * Returns the last non-null element from the provided three elements.
     * If all are {@code null}, it returns {@code null}.
     *
     * @param a the last element to evaluate
     * @param b the second element to evaluate
     * @param c the third element to evaluate
     * @return the last non-null element, or {@code null} if all are {@code null}
     * @see N#lastNonNull(Object, Object, Object)
     */
    @Beta
    public static <T> T lastNonNull(final T a, final T b, final T c) {
        return c != null ? c : (b != null ? b : a);
    }

    /**
     * Returns the last non-null element from the provided array of elements.
     * If the array is {@code null} or empty, or all elements are null, it returns {@code null}.
     *
     * @param a the array of elements to evaluate
     * @return the last non-null element, or {@code null} if the array is {@code null} or empty
     * @see N#lastNonNull(Object[])
     */
    @Beta
    @SafeVarargs
    public static <T> T lastNonNull(final T... a) {
        if (N.isEmpty(a)) {
            return null;
        }

        for (int i = a.length - 1; i >= 0; i--) {
            if (a[i] != null) {
                return a[i];
            }
        }

        return null;
    }

    /**
     * Returns the last non-null element from the provided iterable.
     * If the iterable is {@code null} or empty, or all elements are null, it returns {@code null}.
     *
     * @param c the iterable of elements to evaluate
     * @return the last non-null element, or {@code null} if the iterable is {@code null} or empty
     * @see N#lastNonNull(Iterable)
     * @see Iterables#lastNonNullOrDefault(Iterable, Object)
     */
    @Beta
    public static <T> T lastNonNull(final Iterable<? extends T> c) {
        if (N.isEmpty(c)) {
            return null;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i) != null) {
                    return list.get(i);
                }
            }

            return null;
        }

        final Iterator<T> descendingIterator = N.getDescendingIteratorIfPossible(c);

        if (descendingIterator != null) {
            T next = null;

            while (descendingIterator.hasNext()) {
                if ((next = descendingIterator.next()) != null) {
                    return next;
                }
            }

            return null;
        }

        return lastNonNull(c.iterator());
    }

    /**     
     * Returns the last non-null element from the provided iterator.
     * If the iterator is {@code null} or empty, or all elements are null, it returns {@code null}.
     *
     * @param iter the iterator of elements to evaluate
     * @return the last non-null element, or {@code null} if the iterator is {@code null} or empty
     * @see N#lastNonNull(Iterator)
     * @see Iterables#lastNonNullOrDefault(Iterator, Object)
     */
    @Beta
    public static <T> T lastNonNull(final Iterator<? extends T> iter) {
        if (iter == null) {
            return null;
        }

        T e = null;
        T lastNonNull = null;

        while (iter.hasNext()) {
            if ((e = iter.next()) != null) {
                lastNonNull = e;
            }
        }

        return lastNonNull;
    }

    /**
     * Returns the last non-null element of the given iterable if it is not empty, otherwise returns the specified default value.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the array to check
     * @param defaultValue the default value to return if the iterable is empty
     * @return the last non-null element of the given  iterable if it is not empty, otherwise the specified default value
     * @see #lastNonNullOrDefault(Iterator, Object)
     * @see N#lastNonNull(Iterable)
     */
    @Beta
    public static <T> T lastNonNullOrDefault(final Iterable<? extends T> c, T defaultValue) {
        N.requireNonNull(defaultValue, cs.defaultValue);

        final T ret = lastNonNull(c);

        return ret == null ? defaultValue : ret;
    }

    /**
     * Returns the last non-null element of the given iterator if it is not empty, otherwise returns the specified default value.
     *
     * @param <T> the type of the elements in the iterator
     * @param iter the iterator to check
     * @param defaultValue the default value to return if the iterator is empty
     * @return the last non-null element of the given iterator if it is not empty, otherwise the specified default value
     * @see #lastNonNullOrDefault(Iterable, Object)
     * @see N#lastNonNull(Iterator) 
     */
    @Beta
    public static <T> T lastNonNullOrDefault(final Iterator<? extends T> iter, T defaultValue) {
        N.requireNonNull(defaultValue, cs.defaultValue);

        final T ret = lastNonNull(iter);

        return ret == null ? defaultValue : ret;
    }

    /**
     * Returns the minimum value from the provided array of characters.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalChar}.
     *
     * @param a the array of characters to evaluate
     * @return an {@code OptionalChar} containing the minimum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalChar}
     * @see N#min(char...)
     */
    public static OptionalChar min(final char... a) {
        return a == null || a.length == 0 ? OptionalChar.empty() : OptionalChar.of(N.min(a));
    }

    /**
     * Returns the minimum value from the provided array of bytes.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalByte}.
     *
     * @param a the array of bytes to evaluate
     * @return an {@code OptionalByte} containing the minimum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalByte}
     * @see N#min(byte...)
     */
    public static OptionalByte min(final byte... a) {
        return a == null || a.length == 0 ? OptionalByte.empty() : OptionalByte.of(N.min(a));
    }

    /**
     * Returns the minimum value from the provided array of shorts.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalShort}.
     *
     * @param a the array of shorts to evaluate
     * @return an {@code OptionalShort} containing the minimum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalShort}
     * @see N#min(short...)
     */
    public static OptionalShort min(final short... a) {
        return a == null || a.length == 0 ? OptionalShort.empty() : OptionalShort.of(N.min(a));
    }

    /**
     * Returns the minimum value from the provided array of integers.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalInt}.
     *
     * @param a the array of integers to evaluate
     * @return an  {@code OptionalInt} containing the minimum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalInt}
     * @see N#min(int...)
     */
    public static OptionalInt min(final int... a) {
        return a == null || a.length == 0 ? OptionalInt.empty() : OptionalInt.of(N.min(a));
    }

    /**
     * Returns the minimum value from the provided array of longs.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalLong}.
     *
     * @param a the array of longs to evaluate
     * @return an {@code OptionalLong} containing the minimum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalLong}
     * @see N#min(long...)
     */
    public static OptionalLong min(final long... a) {
        return a == null || a.length == 0 ? OptionalLong.empty() : OptionalLong.of(N.min(a));
    }

    /**
     * Returns the minimum value from the provided array of floats.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalFloat}.
     *
     * @param a the array of floats to evaluate
     * @return an {@code OptionalFloat} containing the minimum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalFloat}
     * @see N#min(float...)
     */
    public static OptionalFloat min(final float... a) {
        return a == null || a.length == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.min(a));
    }

    /**
     * Returns the minimum value from the provided array of doubles.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param a the array of doubles to evaluate
     * @return an {@code OptionalDouble} containing the minimum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#min(double...)
     */
    public static OptionalDouble min(final double... a) {
        return a == null || a.length == 0 ? OptionalDouble.empty() : OptionalDouble.of(N.min(a));
    }

    /**
     * Returns the minimum value from the provided array of elements based on their natural ordering.
     * Null values are considered to be maximum value.
     * If the array is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param a the array of elements to evaluate
     * @return a {@code Nullable} containing the minimum value if the array is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#min(Comparable...)
     */
    public static <T extends Comparable<? super T>> Nullable<T> min(final T[] a) {
        return N.isEmpty(a) ? Nullable.empty() : Nullable.of(N.min(a));
    }

    /**
     * Returns the minimum value from the provided array of elements according to the provided comparator.
     * If the array is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param a the array of elements to evaluate
     * @param cmp the comparator to determine the order of the elements
     * @return a {@code Nullable} containing the minimum value if the array is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#min(Object[], Comparator)
     */
    public static <T> Nullable<T> min(final T[] a, final Comparator<? super T> cmp) {
        return N.isEmpty(a) ? Nullable.empty() : Nullable.of(N.min(a, cmp));
    }

    /**
     * Returns the minimum value from the provided iterable of elements based on their natural ordering.
     * Null values are considered to be maximum value.
     * If the iterable is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param c the iterable of elements to evaluate
     * @return a {@code Nullable} containing the minimum value if the iterable is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#min(Iterable)
     */
    public static <T extends Comparable<? super T>> Nullable<T> min(final Iterable<? extends T> c) {
        return min(c, N.NULL_MAX_COMPARATOR);
    }

    /**
     * Returns the minimum value from the provided iterable of elements according to the provided comparator.
     * If the iterable is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param c the iterable of elements to
     * @param cmp the comparator to determine the order of the elements
     * @return a {@code Nullable} containing the minimum value if the iterable is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#min(Iterable, Comparator)
     */
    public static <T> Nullable<T> min(final Iterable<? extends T> c, final Comparator<? super T> cmp) {
        return c == null ? Nullable.empty() : min(c.iterator(), cmp);
    }

    /**
     * Returns the minimum value from the provided iterator of elements based on their natural ordering.
     * Null values are considered to be maximum value.
     * If the iterator is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param iter the iterator of elements to evaluate
     * @return a {@code Nullable} containing the minimum value if the iterator is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#min(Iterator)
     */
    public static <T extends Comparable<? super T>> Nullable<T> min(final Iterator<? extends T> iter) {
        return min(iter, N.NULL_MAX_COMPARATOR);
    }

    /**
     * Returns the minimum value from the provided iterator of elements according to the provided comparator.
     * If the iterator is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param iter the iterator of elements to evaluate
     * @param cmp the comparator to determine the order of the elements
     * @return a {@code Nullable} containing the minimum value if the iterator is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#min(Iterator, Comparator)
     */
    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    public static <T> Nullable<T> min(final Iterator<? extends T> iter, Comparator<? super T> cmp) {
        cmp = cmp == null ? (Comparator<T>) N.NULL_MAX_COMPARATOR : cmp;
        final boolean isNullMinComparator = cmp == N.NULL_MIN_COMPARATOR;

        if (iter == null || !iter.hasNext()) {
            return Nullable.empty();
        }

        T candidate = iter.next();
        T next = null;

        while (iter.hasNext()) {
            next = iter.next();

            if (isNullMinComparator && next == null) { // NOSONAR
                //noinspection ConstantValue
                return Nullable.of(next);
            } else if (cmp.compare(next, candidate) < 0) {
                candidate = next;
            }
        }

        return Nullable.of(candidate);
    }

    /**
     * Returns the minimum value from the provided array of elements according to the key extracted by the {@code keyExtractor} function.
     * Null values are considered to be maximum value.
     * If the array is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param a the array of elements to evaluate
     * @param keyExtractor the function to transform the elements into a comparable type for comparison
     * @return a {@code Nullable} containing the minimum value if the array is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#min(Object[], Comparator)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> minBy(final T[] a, final Function<? super T, ? extends Comparable> keyExtractor) {
        return min(a, Comparators.nullsLastBy(keyExtractor));
    }

    /**
     * Returns the minimum value from the provided array of elements according to the key extracted by the {@code keyExtractor} function.
     * Null values are considered to be maximum value.
     * If the iterable is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param c the iterable of elements to evaluate
     * @param keyExtractor the function to transform the elements into a comparable type for comparison
     * @return a {@code Nullable} containing the minimum value if the iterable is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#min(Iterable, Comparator)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> minBy(final Iterable<? extends T> c, final Function<? super T, ? extends Comparable> keyExtractor) {
        return min(c, Comparators.nullsLastBy(keyExtractor));
    }

    /**
     * Returns the minimum value from the provided array of elements according to the key extracted by the {@code keyExtractor} function.
     * Null values are considered to be maximum value.
     * If the iterator is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param iter the iterator of elements to evaluate
     * @param keyExtractor the function to transform the elements into a comparable type for comparison
     * @return a {@code Nullable} containing the minimum value if the iterator is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#min(Iterator, Comparator)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> minBy(final Iterator<? extends T> iter, final Function<? super T, ? extends Comparable> keyExtractor) {
        return min(iter, Comparators.nullsLastBy(keyExtractor));
    }

    /**
     * Returns the minimum integer value extracted from the elements in the provided array by the input {@code valueExtractor} function.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalInt}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param valueExtractor the function to extract an integer value from each element
     * @return an {@code OptionalInt} containing the minimum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalInt}
     * @see N#minIntOrDefaultIfEmpty(Object[], ToIntFunction, int)
     */
    @Beta
    public static <T> OptionalInt minInt(final T[] a, final ToIntFunction<? super T> valueExtractor) {
        if (N.isEmpty(a)) {
            return OptionalInt.empty();
        }

        int candidate = valueExtractor.applyAsInt(a[0]);
        int next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsInt(a[i]);

            if (next < candidate) {
                candidate = next;
            }
        }

        return OptionalInt.of(candidate);
    }

    /**
     * Returns the minimum integer value extracted from the elements in the provided iterable by the input {@code valueExtractor} function.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalInt}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param valueExtractor the function to extract an integer value from each element
     * @return an {@code OptionalInt} containing the minimum value if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalInt}
     * @see N#minIntOrDefaultIfEmpty(Iterable, ToIntFunction, int)
     */
    @Beta
    public static <T> OptionalInt minInt(final Iterable<? extends T> c, final ToIntFunction<? super T> valueExtractor) {
        if (c == null) {
            return OptionalInt.empty();
        }

        return minInt(c.iterator(), valueExtractor);
    }

    /**
     * Returns the minimum integer value extracted from the elements in the provided iterator by the input {@code valueExtractor} function.
     * If the iterator is {@code null} or empty, it returns an empty {@code OptionalInt}.
     *
     * @param <T> the type of the elements in the iterator
     * @param iter the iterator of elements to evaluate
     * @param valueExtractor the function to extract an integer value from each element
     * @return an {@code OptionalInt} containing the minimum value if the iterator is not {@code null} or empty, otherwise an empty {@code OptionalInt}
     * @see N#minIntOrDefaultIfEmpty(Iterator, ToIntFunction, int)
     */
    @Beta
    public static <T> OptionalInt minInt(final Iterator<? extends T> iter, final ToIntFunction<? super T> valueExtractor) {
        if (iter == null || !iter.hasNext()) {
            return OptionalInt.empty();
        }

        int candidate = valueExtractor.applyAsInt(iter.next());
        int next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsInt(iter.next());

            if (next < candidate) {
                candidate = next;
            }
        }

        return OptionalInt.of(candidate);
    }

    /**
     * Returns the minimum long value extracted from the elements in the provided array by the input {@code valueExtractor} function.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalLong}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param valueExtractor the function to extract a long value from each element
     * @return an {@code OptionalLong} containing the minimum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalLong}
     * @see N#minLongOrDefaultIfEmpty(Object[], ToLongFunction, long)
     */
    @Beta
    public static <T> OptionalLong minLong(final T[] a, final ToLongFunction<? super T> valueExtractor) {
        if (N.isEmpty(a)) {
            return OptionalLong.empty();
        }

        long candidate = valueExtractor.applyAsLong(a[0]);
        long next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsLong(a[i]);

            if (next < candidate) {
                candidate = next;
            }
        }

        return OptionalLong.of(candidate);
    }

    /**
     * Returns the minimum long value extracted from the elements in the provided iterable by the input {@code valueExtractor} function.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalLong}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param valueExtractor the function to extract a long value from each element
     * @return an {@code OptionalLong} containing the minimum value if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalLong}
     * @see N#minLongOrDefaultIfEmpty(Iterable, ToLongFunction, long)
     */
    @Beta
    public static <T> OptionalLong minLong(final Iterable<? extends T> c, final ToLongFunction<? super T> valueExtractor) {
        if (c == null) {
            return OptionalLong.empty();
        }

        return minLong(c.iterator(), valueExtractor);
    }

    /**
     * Returns the minimum long value extracted from the elements in the provided iterator by the input {@code valueExtractor} function.
     * If the iterator is {@code null} or empty, it returns an empty {@code OptionalLong}.
     *
     * @param <T> the type of the elements in the iterator
     * @param iter the iterator of elements to evaluate
     * @param valueExtractor the function to extract a long value from each element
     * @return an {@code OptionalLong} containing the minimum value if the iterator is not {@code null} or empty, otherwise an empty {@code OptionalLong}
     * @see N#minLongOrDefaultIfEmpty(Iterator, ToLongFunction, long)
     */
    @Beta
    public static <T> OptionalLong minLong(final Iterator<? extends T> iter, final ToLongFunction<? super T> valueExtractor) {
        if (iter == null || !iter.hasNext()) {
            return OptionalLong.empty();
        }

        long candidate = valueExtractor.applyAsLong(iter.next());
        long next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsLong(iter.next());

            if (next < candidate) {
                candidate = next;
            }
        }

        return OptionalLong.of(candidate);
    }

    /**
     * Returns the minimum double value extracted from the elements in the provided array by the input {@code valueExtractor} function.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param valueExtractor the function to extract a double value from each element
     * @return an {@code OptionalDouble} containing the minimum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#minDoubleOrDefaultIfEmpty(Object[], ToDoubleFunction, double)
     */
    @Beta
    public static <T> OptionalDouble minDouble(final T[] a, final ToDoubleFunction<? super T> valueExtractor) {
        if (N.isEmpty(a)) {
            return OptionalDouble.empty();
        }

        double candidate = valueExtractor.applyAsDouble(a[0]);
        double next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsDouble(a[i]);

            if (N.compare(next, candidate) < 0) {
                candidate = next;
            }
        }

        return OptionalDouble.of(candidate);
    }

    /**
     * Returns the minimum double value extracted from the elements in the provided iterable by the input {@code valueExtractor} function.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param valueExtractor the function to extract a double value from each element
     * @return an {@code OptionalDouble} containing the minimum value if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#minDoubleOrDefaultIfEmpty(Iterable, ToDoubleFunction, double)
     */
    @Beta
    public static <T> OptionalDouble minDouble(final Iterable<? extends T> c, final ToDoubleFunction<? super T> valueExtractor) {
        if (c == null) {
            return OptionalDouble.empty();
        }

        return minDouble(c.iterator(), valueExtractor);
    }

    /**
     * Returns the minimum double value extracted from the elements in the provided iterator by the input {@code valueExtractor} function.
     * If the iterator is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the iterator
     * @param iter the iterator of elements to evaluate
     * @param valueExtractor the function to extract a double value from each element
     * @return an {@code OptionalDouble} containing the minimum value if the iterator is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#minDoubleOrDefaultIfEmpty(Iterator, ToDoubleFunction, double)
     */
    @Beta
    public static <T> OptionalDouble minDouble(final Iterator<? extends T> iter, final ToDoubleFunction<? super T> valueExtractor) {
        if (iter == null || !iter.hasNext()) {
            return OptionalDouble.empty();
        }

        double candidate = valueExtractor.applyAsDouble(iter.next());
        double next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsDouble(iter.next());

            if (N.compare(next, candidate) < 0) {
                candidate = next;
            }
        }

        return OptionalDouble.of(candidate);
    }

    /**
     * Returns the maximum value from the provided array of characters.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalChar}.
     *
     * @param a the array of characters to evaluate
     * @return an {@code OptionalChar} containing the maximum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalChar}
     * @see N#max(char...)
     */
    public static OptionalChar max(final char... a) {
        return a == null || a.length == 0 ? OptionalChar.empty() : OptionalChar.of(N.max(a));
    }

    /**
     * Returns the maximum value from the provided array of bytes.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalByte}.
     *
     * @param a the array of bytes to evaluate
     * @return an {@code OptionalByte} containing the maximum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalByte}
     * @see N#max(byte...)
     */
    public static OptionalByte max(final byte... a) {
        return a == null || a.length == 0 ? OptionalByte.empty() : OptionalByte.of(N.max(a));
    }

    /**
     * Returns the maximum value from the provided array of shorts.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalShort}.
     *
     * @param a the array of shorts to evaluate
     * @return an {@code OptionalShort} containing the maximum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalShort}
     * @see N#max(short...)
     */
    public static OptionalShort max(final short... a) {
        return a == null || a.length == 0 ? OptionalShort.empty() : OptionalShort.of(N.max(a));
    }

    /**
     * Returns the maximum value from the provided array of integers.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalInt}.
     *
     * @param a the array of integers to evaluate
     * @return an  {@code OptionalInt} containing the maximum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalInt}
     * @see N#max(int...)
     */
    public static OptionalInt max(final int... a) {
        return a == null || a.length == 0 ? OptionalInt.empty() : OptionalInt.of(N.max(a));
    }

    /**
     * Returns the maximum value from the provided array of longs.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalLong}.
     *
     * @param a the array of longs to evaluate
     * @return an {@code OptionalLong} containing the maximum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalLong}
     * @see N#max(long...)
     */
    public static OptionalLong max(final long... a) {
        return a == null || a.length == 0 ? OptionalLong.empty() : OptionalLong.of(N.max(a));
    }

    /**
     * Returns the maximum value from the provided array of floats.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalFloat}.
     *
     * @param a the array of floats to evaluate
     * @return an {@code OptionalFloat} containing the maximum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalFloat}
     * @see N#max(float...)
     */
    public static OptionalFloat max(final float... a) {
        return a == null || a.length == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.max(a));
    }

    /**
     * Returns the maximum value from the provided array of doubles.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param a the array of doubles to evaluate
     * @return an {@code OptionalDouble} containing the maximum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#max(double...)
     */
    public static OptionalDouble max(final double... a) {
        return a == null || a.length == 0 ? OptionalDouble.empty() : OptionalDouble.of(N.max(a));
    }

    /**
     * Returns the maximum value from the provided array of elements based on their natural ordering.
     * Null values are considered to be minimum
     * If the array is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param a the array of elements to evaluate
     * @return a {@code Nullable} containing the maximum value if the array is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#max(Comparable...)
     */
    public static <T extends Comparable<? super T>> Nullable<T> max(final T[] a) {
        return N.isEmpty(a) ? Nullable.empty() : Nullable.of(N.max(a));
    }

    /**
     * Returns the maximum value from the provided array of elements according to the provided comparator.
     * If the array is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param a the array of elements to evaluate
     * @param cmp the comparator to determine the order of the elements
     * @return a {@code Nullable} containing the maximum value if the array is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#max(Object[], Comparator)
     */
    public static <T> Nullable<T> max(final T[] a, final Comparator<? super T> cmp) {
        return N.isEmpty(a) ? Nullable.empty() : Nullable.of(N.max(a, cmp));
    }

    /**
     * Returns the maximum value from the provided iterable of elements based on their natural ordering.
     * Null values are considered to be minimum
     * If the iterable is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param c the iterable of elements to evaluate
     * @return a {@code Nullable} containing the maximum value if the iterable is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#max(Iterable)
     */
    public static <T extends Comparable<? super T>> Nullable<T> max(final Iterable<? extends T> c) {
        return max(c, N.NULL_MIN_COMPARATOR);
    }

    /**
     * Returns the maximum value from the provided iterable of elements according to the provided comparator.
     * If the iterable is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param c the iterable of elements to
     * @param cmp the comparator to determine the order of the elements
     * @return a {@code Nullable} containing the maximum value if the iterable is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#max(Iterable, Comparator)
     */
    public static <T> Nullable<T> max(final Iterable<? extends T> c, final Comparator<? super T> cmp) {
        return c == null ? Nullable.empty() : max(c.iterator(), cmp);
    }

    /**
     * Returns the maximum value from the provided iterator of elements based on their natural ordering.
     * Null values are considered to be minimum
     * If the iterator is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param iter the iterator of elements to evaluate
     * @return a {@code Nullable} containing the maximum value if the iterator is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#max(Iterator)
     */
    public static <T extends Comparable<? super T>> Nullable<T> max(final Iterator<? extends T> iter) {
        return max(iter, N.NULL_MIN_COMPARATOR);
    }

    /**
     * Returns the maximum value from the provided iterator of elements according to the provided comparator.
     * If the iterator is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param iter the iterator of elements to evaluate
     * @param cmp the comparator to determine the order of the elements
     * @return a {@code Nullable} containing the maximum value if the iterator is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#max(Iterator, Comparator)
     */
    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    public static <T> Nullable<T> max(final Iterator<? extends T> iter, Comparator<? super T> cmp) {
        cmp = cmp == null ? (Comparator<T>) N.NULL_MIN_COMPARATOR : cmp;
        final boolean isNullMaxComparator = cmp == N.NULL_MAX_COMPARATOR;

        if (iter == null || !iter.hasNext()) {
            return Nullable.empty();
        }

        T candidate = iter.next();
        T next = null;

        while (iter.hasNext()) {
            next = iter.next();

            if (isNullMaxComparator && next == null) { // NOSONAR
                //noinspection ConstantValue
                return Nullable.of(next);
            } else if (cmp.compare(next, candidate) > 0) {
                candidate = next;
            }
        }

        return Nullable.of(candidate);
    }

    /**
     * Returns the maximum value from the provided array of elements according to the key extracted by the {@code keyExtractor} function.
     * Null values are considered to be minimum
     * If the array is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param a the array of elements to evaluate
     * @param keyExtractor the function to transform the elements into a comparable type for comparison
     * @return a {@code Nullable} containing the maximum value if the array is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#max(Object[], Comparator)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> maxBy(final T[] a, final Function<? super T, ? extends Comparable> keyExtractor) {
        return max(a, Comparators.nullsFirstBy(keyExtractor));
    }

    /**
     * Returns the maximum value from the provided array of elements according to the key extracted by the {@code keyExtractor} function.
     * Null values are considered to be minimum
     * If the iterable is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param c the iterable of elements to evaluate
     * @param keyExtractor the function to transform the elements into a comparable type for comparison
     * @return a {@code Nullable} containing the maximum value if the iterable is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#max(Iterable, Comparator)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> maxBy(final Iterable<? extends T> c, final Function<? super T, ? extends Comparable> keyExtractor) {
        return max(c, Comparators.nullsFirstBy(keyExtractor));
    }

    /**
     * Returns the maximum value from the provided array of elements according to the key extracted by the {@code keyExtractor} function.
     * Null values are considered to be minimum
     * If the iterator is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param iter the iterator of elements to evaluate
     * @param keyExtractor the function to transform the elements into a comparable type for comparison
     * @return a {@code Nullable} containing the maximum value if the iterator is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#max(Iterator, Comparator)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Nullable<T> maxBy(final Iterator<? extends T> iter, final Function<? super T, ? extends Comparable> keyExtractor) {
        return max(iter, Comparators.nullsFirstBy(keyExtractor));
    }

    /**
     * Returns the maximum integer value extracted from the elements in the provided array by the input {@code valueExtractor} function.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalInt}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param valueExtractor the function to extract an integer value from each element
     * @return an {@code OptionalInt} containing the maximum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalInt}
     * @see N#maxIntOrDefaultIfEmpty(Object[], ToIntFunction, int)
     */
    @Beta
    public static <T> OptionalInt maxInt(final T[] a, final ToIntFunction<? super T> valueExtractor) {
        if (N.isEmpty(a)) {
            return OptionalInt.empty();
        }

        int candidate = valueExtractor.applyAsInt(a[0]);
        int next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsInt(a[i]);

            if (next > candidate) {
                candidate = next;
            }
        }

        return OptionalInt.of(candidate);
    }

    /**
     * Returns the maximum integer value extracted from the elements in the provided iterable by the input {@code valueExtractor} function.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalInt}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param valueExtractor the function to extract an integer value from each element
     * @return an {@code OptionalInt} containing the maximum value if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalInt}
     * @see N#maxIntOrDefaultIfEmpty(Iterable, ToIntFunction, int)
     */
    @Beta
    public static <T> OptionalInt maxInt(final Iterable<? extends T> c, final ToIntFunction<? super T> valueExtractor) {
        if (c == null) {
            return OptionalInt.empty();
        }

        return maxInt(c.iterator(), valueExtractor);
    }

    /**
     * Returns the maximum integer value extracted from the elements in the provided iterator by the input {@code valueExtractor} function.
     * If the iterator is {@code null} or empty, it returns an empty {@code OptionalInt}.
     *
     * @param <T> the type of the elements in the iterator
     * @param iter the iterator of elements to evaluate
     * @param valueExtractor the function to extract an integer value from each element
     * @return an {@code OptionalInt} containing the maximum value if the iterator is not {@code null} or empty, otherwise an empty {@code OptionalInt}
     * @see N#maxIntOrDefaultIfEmpty(Iterator, ToIntFunction, int)
     */
    @Beta
    public static <T> OptionalInt maxInt(final Iterator<? extends T> iter, final ToIntFunction<? super T> valueExtractor) {
        if (iter == null || !iter.hasNext()) {
            return OptionalInt.empty();
        }

        int candidate = valueExtractor.applyAsInt(iter.next());
        int next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsInt(iter.next());

            if (next > candidate) {
                candidate = next;
            }
        }

        return OptionalInt.of(candidate);
    }

    /**
     * Returns the maximum long value extracted from the elements in the provided array by the input {@code valueExtractor} function.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalLong}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param valueExtractor the function to extract a long value from each element
     * @return an {@code OptionalLong} containing the maximum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalLong}
     * @see N#maxLongOrDefaultIfEmpty(Object[], ToLongFunction, long)
     */
    @Beta
    public static <T> OptionalLong maxLong(final T[] a, final ToLongFunction<? super T> valueExtractor) {
        if (N.isEmpty(a)) {
            return OptionalLong.empty();
        }

        long candidate = valueExtractor.applyAsLong(a[0]);
        long next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsLong(a[i]);

            if (next > candidate) {
                candidate = next;
            }
        }

        return OptionalLong.of(candidate);
    }

    /**
     * Returns the maximum long value extracted from the elements in the provided iterable by the input {@code valueExtractor} function.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalLong}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param valueExtractor the function to extract a long value from each element
     * @return an {@code OptionalLong} containing the maximum value if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalLong}
     * @see N#maxLongOrDefaultIfEmpty(Iterable, ToLongFunction, long)
     */
    @Beta
    public static <T> OptionalLong maxLong(final Iterable<? extends T> c, final ToLongFunction<? super T> valueExtractor) {
        if (c == null) {
            return OptionalLong.empty();
        }

        return maxLong(c.iterator(), valueExtractor);
    }

    /**
     * Returns the maximum long value extracted from the elements in the provided iterator by the input {@code valueExtractor} function.
     * If the iterator is {@code null} or empty, it returns an empty {@code OptionalLong}.
     *
     * @param <T> the type of the elements in the iterator
     * @param iter the iterator of elements to evaluate
     * @param valueExtractor the function to extract a long value from each element
     * @return an {@code OptionalLong} containing the maximum value if the iterator is not {@code null} or empty, otherwise an empty {@code OptionalLong}
     * @see N#maxLongOrDefaultIfEmpty(Iterator, ToLongFunction, long)
     */
    @Beta
    public static <T> OptionalLong maxLong(final Iterator<? extends T> iter, final ToLongFunction<? super T> valueExtractor) {
        if (iter == null || !iter.hasNext()) {
            return OptionalLong.empty();
        }

        long candidate = valueExtractor.applyAsLong(iter.next());
        long next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsLong(iter.next());

            if (next > candidate) {
                candidate = next;
            }
        }

        return OptionalLong.of(candidate);
    }

    /**
     * Returns the maximum double value extracted from the elements in the provided array by the input {@code valueExtractor} function.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param valueExtractor the function to extract a double value from each element
     * @return an {@code OptionalDouble} containing the maximum value if the array is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#maxDoubleOrDefaultIfEmpty(Object[], ToDoubleFunction, double)
     */
    @Beta
    public static <T> OptionalDouble maxDouble(final T[] a, final ToDoubleFunction<? super T> valueExtractor) {
        if (N.isEmpty(a)) {
            return OptionalDouble.empty();
        }

        double candidate = valueExtractor.applyAsDouble(a[0]);
        double next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsDouble(a[i]);

            if (N.compare(next, candidate) > 0) {
                candidate = next;
            }
        }

        return OptionalDouble.of(candidate);
    }

    /**
     * Returns the maximum double value extracted from the elements in the provided iterable by the input {@code valueExtractor} function.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param valueExtractor the function to extract a double value from each element
     * @return an {@code OptionalDouble} containing the maximum value if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#maxDoubleOrDefaultIfEmpty(Iterable, ToDoubleFunction, double)
     */
    @Beta
    public static <T> OptionalDouble maxDouble(final Iterable<? extends T> c, final ToDoubleFunction<? super T> valueExtractor) {
        if (c == null) {
            return OptionalDouble.empty();
        }

        return maxDouble(c.iterator(), valueExtractor);
    }

    /**
     * Returns the maximum double value extracted from the elements in the provided iterator by the input {@code valueExtractor} function.
     * If the iterator is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the iterator
     * @param iter the iterator of elements to evaluate
     * @param valueExtractor the function to extract a double value from each element
     * @return an {@code OptionalDouble} containing the maximum value if the iterator is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#maxDoubleOrDefaultIfEmpty(Iterator, ToDoubleFunction, double)
     */
    @Beta
    public static <T> OptionalDouble maxDouble(final Iterator<? extends T> iter, final ToDoubleFunction<? super T> valueExtractor) {
        if (iter == null || !iter.hasNext()) {
            return OptionalDouble.empty();
        }

        double candidate = valueExtractor.applyAsDouble(iter.next());
        double next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsDouble(iter.next());

            if (N.compare(next, candidate) > 0) {
                candidate = next;
            }
        }

        return OptionalDouble.of(candidate);
    }

    /**
     * Returns the minimum and maximum values from the provided array of elements based on their natural ordering.
     * The result is wrapped in an Optional Pair, where the first element is the minimum and the second is the maximum.
     * If the array is {@code null} or empty, it returns an empty Optional.
     *
     * @param a the array of elements to evaluate
     * @return an Optional Pair containing the minimum and maximum values if the array is not {@code null} or empty, otherwise an empty Optional
     * @see N#minMax(Comparable...)
     */
    public static <T extends Comparable<? super T>> Optional<Pair<T, T>> minMax(final T[] a) {
        return N.isEmpty(a) ? Optional.empty() : Optional.of(N.minMax(a));
    }

    /**
     * Returns the minimum and maximum values from the provided array of elements according to the provided comparator.
     * The result is wrapped in an Optional Pair, where the first element is the minimum and the second is the maximum.
     * If the array is {@code null} or empty, it returns an empty Optional.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param cmp the comparator to determine the order of the elements
     * @return an Optional Pair containing the minimum and maximum values if the array is not {@code null} or empty, otherwise an empty Optional
     * @see N#minMax(Object[], Comparator)
     */
    public static <T> Optional<Pair<T, T>> minMax(final T[] a, final Comparator<? super T> cmp) {
        return N.isEmpty(a) ? Optional.empty() : Optional.of(N.minMax(a, cmp));
    }

    /**
     * Returns the minimum and maximum values from the provided iterable of elements based on their natural ordering.
     * The result is wrapped in an Optional Pair, where the first element is the minimum and the second is the maximum.
     * If the iterable is {@code null} or empty, it returns an empty Optional.
     *
     * @param c the iterable of elements to evaluate
     * @return an Optional Pair containing the minimum and maximum values if the iterable is not {@code null} or empty, otherwise an empty Optional
     * @see N#minMax(Iterable)
     */
    public static <T extends Comparable<? super T>> Optional<Pair<T, T>> minMax(final Iterable<? extends T> c) {
        final Iterator<? extends T> iter = c == null ? null : c.iterator();

        return iter == null || !iter.hasNext() ? Optional.empty() : Optional.of(N.minMax(iter));
    }

    /**
     * Returns the minimum and maximum values from the provided iterable of elements, according to the provided comparator.
     * The result is wrapped in an Optional Pair, where the first element is the minimum and the second is the maximum.
     * If the iterable is {@code null} or empty, it returns an empty Optional.
     *
     * @param c the iterable of elements to evaluate
     * @param cmp the comparator to determine the order of the elements
     * @return an Optional Pair containing the minimum and maximum values if the iterable is not {@code null} or empty, otherwise an empty Optional
     * @see N#minMax(Iterable, Comparator)
     */
    public static <T> Optional<Pair<T, T>> minMax(final Iterable<? extends T> c, final Comparator<? super T> cmp) {
        final Iterator<? extends T> iter = c == null ? null : c.iterator();

        return iter == null || !iter.hasNext() ? Optional.empty() : Optional.of(N.minMax(iter, cmp));
    }

    /**
     * Returns the minimum and maximum values from the provided iterator of elements based on their natural ordering.
     * The result is wrapped in an Optional Pair, where the first element is the minimum and the second is the maximum.
     * If the iterator is {@code null} or empty, it returns an empty Optional.
     *
     * @param iter the iterator of elements to evaluate
     * @return an Optional Pair containing the minimum and maximum values if the iterator is not {@code null} or empty, otherwise an empty Optional
     * @see N#minMax(Iterator)
     */
    public static <T extends Comparable<? super T>> Optional<Pair<T, T>> minMax(final Iterator<? extends T> iter) {
        return iter == null || !iter.hasNext() ? Optional.empty() : Optional.of(N.minMax(iter));
    }

    /**
     * Returns the minimum and maximum values from the provided iterator of elements, according to the provided comparator.
     * The result is wrapped in an Optional Pair, where the first element is the minimum and the second is the maximum.
     * If the iterator is {@code null} or empty, it returns an empty Optional.
     *
     * @param iter the iterator of elements to evaluate
     * @param cmp the comparator to determine the order of the elements
     * @return an Optional Pair containing the minimum and maximum values if the iterator is not {@code null} or empty, otherwise an empty Optional
     * @see N#minMax(Iterator, Comparator)
     */
    public static <T> Optional<Pair<T, T>> minMax(final Iterator<? extends T> iter, final Comparator<? super T> cmp) {
        return iter == null || !iter.hasNext() ? Optional.empty() : Optional.of(N.minMax(iter, cmp));
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array based on their natural ordering.
     * The result is wrapped in a {@code Nullable}. If the array is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param <T> the type of the elements in the array, which must be a subtype of Comparable
     * @param a the array of elements to evaluate
     * @return a {@code Nullable} containing the median value if the array is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#median(Comparable...)
     * @see Median#of(Comparable[])
     * @see Median#of(Object[], Comparator)
     */
    public static <T extends Comparable<? super T>> Nullable<T> median(final T[] a) {
        return N.isEmpty(a) ? Nullable.empty() : Nullable.of(N.median(a));
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array, according to the provided comparator.
     * The result is wrapped in a {@code Nullable}. If the array is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param <T> the type of the elements in the array, which must be a subtype of Comparable
     * @param a the array of elements to evaluate
     * @param cmp the comparator to determine the order of the elements
     * @return a {@code Nullable} containing the median value if the array is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#median(Object[], Comparator)
     * @see Median#of(Comparable[])
     * @see Median#of(Object[], Comparator)
     */
    public static <T> Nullable<T> median(final T[] a, final Comparator<? super T> cmp) {
        return N.isEmpty(a) ? Nullable.empty() : Nullable.of(N.median(a, cmp));
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified collection based on their natural ordering.
     * The result is wrapped in a {@code Nullable}. If the collection is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param <T> the type of the elements in the collection, which must be a subtype of Comparable
     * @param c the collection of elements to evaluate
     * @return a {@code Nullable} containing the median value if the collection is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#median(Collection)
     * @see Median#of(Collection)
     * @see Median#of(Collection, Comparator)
     */
    public static <T extends Comparable<? super T>> Nullable<T> median(final Collection<? extends T> c) {
        return N.isEmpty(c) ? Nullable.empty() : Nullable.of(N.median(c));
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified collection, according to the provided comparator.
     * The result is wrapped in a {@code Nullable}. If the collection is {@code null} or empty, it returns an empty {@code Nullable}.
     *
     * @param <T> the type of the elements in the collection, which must be a subtype of Comparable
     * @param c the collection of elements to evaluate
     * @param cmp the comparator to determine the order of the elements
     * @return a {@code Nullable} containing the median value if the collection is not {@code null} or empty, otherwise an empty {@code Nullable}
     * @see N#median(Collection, Comparator)
     * @see Median#of(Collection)
     * @see Median#of(Collection, Comparator)
     */
    public static <T> Nullable<T> median(final Collection<? extends T> c, final Comparator<? super T> cmp) {
        return N.isEmpty(c) ? Nullable.empty() : Nullable.of(N.median(c, cmp));
    }

    //    /**
    //     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
    //     *
    //     * @param <T>
    //     * @param a
    //     * @param keyExtractor
    //     * @return
    //     * @deprecated
    //     * @see Comparators#comparingBy(Function)
    //     */
    //    @Deprecated
    //    @Beta
    //    @SuppressWarnings("rawtypes")
    //    public static <T> Nullable<T> medianBy(final T[] a, final Function<? super T, ? extends Comparable> keyExtractor) {
    //        return median(a, Comparators.comparingBy(keyExtractor));
    //    }
    //
    //    /**
    //     * Returns {@code Nullable.empty()} if the specified {@code Array/Collection} is {@code null} or empty.
    //     *
    //     * @param <T>
    //     * @param c
    //     * @param keyExtractor
    //     * @return
    //     * @deprecated
    //     * @see Comparators#comparingBy(Function)
    //     */
    //    @Deprecated
    //    @Beta
    //    @SuppressWarnings("rawtypes")
    //    public static <T> Nullable<T> medianBy(final Collection<? extends T> c, final Function<? super T, ? extends Comparable> keyExtractor) {
    //        return median(c, Comparators.comparingBy(keyExtractor));
    //    }

    /**
     * Returns the <i>k-th</i> largest element from the provided array based on their natural ordering.
     * If the array is {@code null}, empty, or its length is less than {@code k}, it returns an empty {@code Nullable}.
     *
     * @param <T> the type of the elements in the array, which must be a subtype of Comparable
     * @param a the array of elements to evaluate
     * @param k the position of the largest element to find (1-based index)
     * @return a {@code Nullable} containing the <i>k-th</i> largest value if the array is not {@code null}, not empty, and its length is greater or equal to {@code k}, otherwise an empty {@code Nullable}
     * @see N#kthLargest(Comparable[], int)
     */
    public static <T extends Comparable<? super T>> Nullable<T> kthLargest(final T[] a, final int k) {
        return N.isEmpty(a) || a.length < k ? Nullable.empty() : Nullable.of(N.kthLargest(a, k));
    }

    /**
     * Returns the <i>k-th</i> largest element from the provided array according to the provided comparator.
     * If the array is {@code null}, empty, or its length is less than {@code k}, it returns an empty {@code Nullable}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param k the position of the largest element to find (1-based index)
     * @param cmp the comparator to determine the order of the elements
     * @return a {@code Nullable} containing the <i>k-th</i> largest value if the array is not {@code null}, not empty, and its length is greater or equal to {@code k}, otherwise an empty {@code Nullable}
     * @see N#kthLargest(Object[], int, Comparator)
     */
    public static <T> Nullable<T> kthLargest(final T[] a, final int k, final Comparator<? super T> cmp) {
        return N.isEmpty(a) || a.length < k ? Nullable.empty() : Nullable.of(N.kthLargest(a, k, cmp));
    }

    /**
     * Returns the <i>k-th</i> largest element from the provided collection based on their natural ordering.
     * If the collection is {@code null}, empty, or its size is less than {@code k}, it returns an empty {@code Nullable}.
     *
     * @param <T> the type of the elements in the collection, which must be a subtype of Comparable
     * @param c the collection of elements to evaluate
     * @param k the position of the largest element to find (1-based index)
     * @return a {@code Nullable} containing the <i>k-th</i> largest value if the collection is not {@code null}, not empty, and its size is greater or equal to {@code k}, otherwise an empty {@code Nullable}
     * @see N#kthLargest(Collection, int)
     */
    public static <T extends Comparable<? super T>> Nullable<T> kthLargest(final Collection<? extends T> c, final int k) {
        return N.isEmpty(c) || c.size() < k ? Nullable.empty() : Nullable.of(N.kthLargest(c, k));
    }

    /**
     * Returns the <i>k-th</i> largest element from the provided collection based on the provided comparator.
     * If the collection is {@code null}, empty, or its size is less than k, a {@code Nullable}.empty() is returned.
     *
     * @param <T> The type of elements in the collection.
     * @param c The collection from which to find the <i>k-th</i> largest element.
     * @param k The position from the end of a sorted list of the collection's elements (1-based index).
     * @param cmp The comparator used to determine the order of the collection's elements.
     * @return A {@code Nullable} containing the <i>k-th</i> largest element if it exists, otherwise {@code Nullable}.empty().
     * @see N#kthLargest(Collection, int, Comparator)
     */
    public static <T> Nullable<T> kthLargest(final Collection<? extends T> c, final int k, final Comparator<? super T> cmp) {
        return N.isEmpty(c) || c.size() < k ? Nullable.empty() : Nullable.of(N.kthLargest(c, k, cmp));
    }

    /**
     * Returns the sum of the integer value of provided numbers as an {@code OptionalInt}.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalInt}.
     *
     * @param <T> the type of the elements in the iterable, which must be a subtype of Number
     * @param c the iterable of elements to evaluate
     * @return an {@code OptionalInt} containing the sum if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalInt}
     * @see N#sumInt(Iterable)
     */
    public static <T extends Number> OptionalInt sumInt(final Iterable<? extends T> c) {
        return sumInt(c, Fn.numToInt());
    }

    /**
     * Returns the sum of the integer values extracted from the elements in the provided iterable by the input {@code func} function.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalInt}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param func the function to extract an integer value from each element
     * @return an {@code OptionalInt} containing the sum if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalInt}
     * @see N#sumInt(Iterable, ToIntFunction)
     */
    public static <T> OptionalInt sumInt(final Iterable<? extends T> c, final ToIntFunction<? super T> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.empty() : c.iterator();

        if (!iter.hasNext()) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(N.sumInt(c, func));
    }

    /**
     * Returns the sum of the integer values of the provided numbers as an {@code OptionalLong}.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalLong}.
     *
     * @param <T> the type of the elements in the iterable, which must be a subtype of Number
     * @param c the iterable of elements to evaluate
     * @return an {@code OptionalLong} containing the sum if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalLong}
     * @see N#sumIntToLong(Iterable)
     */
    public static <T extends Number> OptionalLong sumIntToLong(final Iterable<? extends T> c) {
        return sumIntToLong(c, Fn.numToInt());
    }

    /**
     * Returns the sum of the integer values extracted from the elements in the provided iterable by the input {@code func} function as an {@code OptionalLong}.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalLong}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param func the function to extract an integer value from each element
     * @return an {@code OptionalLong} containing the sum if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalLong}
     * @see N#sumIntToLong(Iterable, ToIntFunction)
     */
    public static <T> OptionalLong sumIntToLong(final Iterable<? extends T> c, final ToIntFunction<? super T> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.empty() : c.iterator();

        if (!iter.hasNext()) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(N.sumIntToLong(c, func));
    }

    /**
     * Returns the sum of the long values of the provided numbers as an {@code OptionalLong}.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalLong}.
     *
     * @param <T> the type of the elements in the iterable, which must be a subtype of Number
     * @param c the iterable of elements to evaluate
     * @return an {@code OptionalLong} containing the sum if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalLong}
     * @see N#sumLong(Iterable)
     */
    public static <T extends Number> OptionalLong sumLong(final Iterable<? extends T> c) {
        return sumLong(c, Fn.numToLong());
    }

    /**
     * Returns the sum of the long values extracted from the elements in the provided iterable by the input {@code func} function.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalLong}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param func the function to extract a long value from each element
     * @return an {@code OptionalLong} containing the sum if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalLong}
     * @see N#sumLong(Iterable, ToLongFunction)
     */
    public static <T> OptionalLong sumLong(final Iterable<? extends T> c, final ToLongFunction<? super T> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.empty() : c.iterator();

        if (!iter.hasNext()) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(N.sumLong(c, func));
    }

    /**
     * Returns the sum of the double values of the provided numbers as an {@code OptionalDouble}.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the iterable, which must be a subtype of Number
     * @param c the iterable of elements to evaluate
     * @return an {@code OptionalDouble} containing the sum if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#sumDouble(Iterable)
     */
    public static <T extends Number> OptionalDouble sumDouble(final Iterable<? extends T> c) {
        return sumDouble(c, Fn.numToDouble());
    }

    /**
     * Returns the sum of the double values extracted from the elements in the provided iterable by the input {@code func} function.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param func the function to extract a double value from each element
     * @return an {@code OptionalDouble} containing the sum if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#sumDouble(Iterable, ToDoubleFunction)
     */
    public static <T> OptionalDouble sumDouble(final Iterable<? extends T> c, final ToDoubleFunction<? super T> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.empty() : c.iterator();

        if (!iter.hasNext()) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(N.sumDouble(c, func));
    }

    /**
     * Returns the sum of the BigInteger values in the provided iterable.
     * If the iterable is {@code null} or empty, it returns an empty {@code Optional<BigInteger>}.
     *
     * @param c the iterable of BigInteger elements to evaluate
     * @return an {@code Optional<BigInteger>} containing the sum if the iterable is not {@code null} or empty, otherwise an empty {@code Optional<BigInteger>}
     * @see N#sumBigInteger(Iterable)
     */
    public static Optional<BigInteger> sumBigInteger(final Iterable<? extends BigInteger> c) {
        return sumBigInteger(c, Fn.identity());
    }

    /**
     * Returns the sum of the BigInteger values extracted from the elements in the provided iterable by the input {@code func} function.
     * If the iterable is {@code null} or empty, it returns an empty {@code Optional<BigInteger>}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param func the function to extract a BigInteger value from each element
     * @return an {@code Optional<BigInteger>} containing the sum if the iterable is not {@code null} or empty, otherwise an empty {@code Optional<BigInteger>}
     * @see N#sumBigInteger(Iterable, Function)
     */
    public static <T> Optional<BigInteger> sumBigInteger(final Iterable<? extends T> c, final Function<? super T, BigInteger> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.empty() : c.iterator();

        if (!iter.hasNext()) {
            return Optional.empty();
        }

        return Optional.of(N.sumBigInteger(c, func));
    }

    /**
     * Returns the sum of the BigDecimal values in the provided iterable.
     * If the iterable is {@code null} or empty, it returns an empty {@code Optional<BigDecimal>}.
     *
     * @param c the iterable of BigDecimal elements to evaluate
     * @return an {@code Optional<BigDecimal>} containing the sum if the iterable is not {@code null} or empty, otherwise an empty {@code Optional<BigDecimal>}
     * @see N#sumBigDecimal(Iterable)
     */
    public static Optional<BigDecimal> sumBigDecimal(final Iterable<? extends BigDecimal> c) {
        return sumBigDecimal(c, Fn.identity());
    }

    /**
     * Returns the sum of the BigDecimal values extracted from the elements in the provided iterable by the input {@code func} function.
     * If the iterable is {@code null} or empty, it returns an empty {@code Optional<BigDecimal>}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param func the function to extract a BigDecimal value from each element
     * @return an {@code Optional<BigDecimal>} containing the sum if the iterable is not {@code null} or empty, otherwise an empty {@code Optional<BigDecimal>}
     * @see N#sumBigDecimal(Iterable, Function)
     */
    public static <T> Optional<BigDecimal> sumBigDecimal(final Iterable<? extends T> c, final Function<? super T, BigDecimal> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.empty() : c.iterator();

        if (!iter.hasNext()) {
            return Optional.empty();
        }

        return Optional.of(N.sumBigDecimal(c, func));
    }

    /**
     * Returns the average of the integer values of the provided numbers as an {@code OptionalDouble}.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array, which must be a subtype of Number
     * @param a the array of elements to evaluate
     * @return the average of the integer values if the array is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#averageInt(Number[])
     */
    public static <T extends Number> OptionalDouble averageInt(final T[] a) {
        return averageInt(a, Fn.numToInt());
    }

    /**
     * Returns the average of the integer values of the provided numbers in the specified range as an {@code OptionalDouble}.
     * If the specified range is empty ({@code fromIndex == toIndex}, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array, which must be a subtype of Number
     * @param a the array of elements to evaluate
     * @param fromIndex the start index of the range, inclusive
     * @param toIndex the end index of the range, exclusive
     * @return the average of the integer values of the provided numbers in the specified range as an {@code OptionalDouble} if the ranger is not empty, otherwise an empty {@code OptionalDouble}
     * @throws IndexOutOfBoundsException if the range is invalid: ({@code fromIndex < 0 || fromIndex > toIndex || toIndex > a.length})
     * @see N#averageInt(Number[], int, int)
     */
    public static <T extends Number> OptionalDouble averageInt(final T[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        return averageInt(a, fromIndex, toIndex, Fn.numToInt());
    }

    /**
     * Returns the average of the integer values extracted from the elements in the provided array by the input {@code func} function as an {@code OptionalDouble}.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param func the function to extract an integer value from each element
     * @return the average of the integer values if the array is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#averageInt(Object[], ToIntFunction)
     */
    public static <T> OptionalDouble averageInt(final T[] a, final ToIntFunction<? super T> func) {
        if (N.isEmpty(a)) {
            return OptionalDouble.empty();
        }

        return averageInt(a, 0, a.length, func);
    }

    /**
     * Returns the average of the integer values extracted from the elements in the specified range by the input {@code func} function as an {@code OptionalDouble}.
     * If the specified range is empty ({@code fromIndex == toIndex}, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param fromIndex the start index of the range, inclusive
     * @param toIndex the end index of the range, exclusive
     * @param func the function to extract an integer value from each element
     * @return the average of the integer values of the provided numbers in the specified range as an {@code OptionalDouble} if the ranger is not empty, otherwise an empty {@code OptionalDouble}
     * @throws IndexOutOfBoundsException if the range is invalid: ({@code fromIndex < 0 || fromIndex > toIndex || toIndex > a.length})
     * @see N#averageInt(Object[], int, int, ToIntFunction)
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
     * Returns the average of the integer values of the provided numbers in the specified range as an {@code OptionalDouble}.
     * If the specified range is empty ({@code fromIndex == toIndex}, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the collection, which must be a subtype of Number
     * @param c the collection of elements to evaluate
     * @param fromIndex the start index of the range, inclusive
     * @param toIndex the end index of the range, exclusive
     * @return the average of the integer values of the provided numbers in the specified range as an {@code OptionalDouble} if the ranger is not empty, otherwise an empty {@code OptionalDouble}
     * @throws IndexOutOfBoundsException if the range is invalid: ({@code fromIndex < 0 || fromIndex > toIndex || toIndex > a.length})
     * @see N#averageInt(Collection, int, int)
     */
    public static <T extends Number> OptionalDouble averageInt(final Collection<? extends T> c, final int fromIndex, final int toIndex)
            throws IndexOutOfBoundsException {
        return averageInt(c, fromIndex, toIndex, Fn.numToInt());
    }

    /**
     * Returns the average of the integer values extracted from the elements in the specified range by the input {@code func} function as an {@code OptionalDouble}.
     * If the specified range is empty ({@code fromIndex == toIndex}, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the collection
     * @param c the collection of elements to evaluate
     * @param fromIndex the start index of the range, inclusive
     * @param toIndex the end index of the range, exclusive
     * @param func the function to extract an integer value from each element
     * @return the average of the integer values of the provided numbers in the specified range as an {@code OptionalDouble} if the ranger is not empty, otherwise an empty {@code OptionalDouble}
     * @throws IndexOutOfBoundsException if the range is invalid: ({@code fromIndex < 0 || fromIndex > toIndex || toIndex > a.length})
     * @see N#averageInt(Collection, int, int, ToIntFunction)
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
     * Returns the average of the integer values of the provided numbers as an {@code OptionalDouble}.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the iterable, which must be a subtype of Number
     * @param c the iterable of elements to evaluate
     * @return the average of the integer values if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#averageInt(Iterable)
     */
    public static <T extends Number> OptionalDouble averageInt(final Iterable<? extends T> c) {
        return averageInt(c, Fn.numToInt());
    }

    /**
     * Returns the average of the integer values extracted from the elements in the provided iterable by the input {@code func} function as an {@code OptionalDouble}.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param func the function to extract an integer value from each element
     * @return the average of the integer values if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#averageInt(Iterable, ToIntFunction)
     */
    public static <T> OptionalDouble averageInt(final Iterable<? extends T> c, final ToIntFunction<? super T> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.empty() : c.iterator();

        if (!iter.hasNext()) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(N.averageInt(c, func));
    }

    /**
     * Returns the average of the long values of the provided numbers as an {@code OptionalDouble}.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array, which must be a subtype of Number
     * @param a the array of elements to evaluate
     * @return the average of the long values if the array is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#averageLong(Number[])
     */
    public static <T extends Number> OptionalDouble averageLong(final T[] a) {
        return averageLong(a, Fn.numToLong());
    }

    /**
     * Returns the average of the long values of the provided numbers in the specified range as an {@code OptionalDouble}.
     * If the specified range is empty ({@code fromIndex == toIndex}, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array, which must be a subtype of Number
     * @param a the array of elements to evaluate
     * @param fromIndex the start index of the range, inclusive
     * @param toIndex the end index of the range, exclusive
     * @return the average of the long values of the provided numbers in the specified range as an {@code OptionalDouble} if the ranger is not empty, otherwise an empty {@code OptionalDouble}
     * @throws IndexOutOfBoundsException if the range is invalid: ({@code fromIndex < 0 || fromIndex > toIndex || toIndex > a.length})
     * @see N#averageLong(Number[], int, int)
     */
    public static <T extends Number> OptionalDouble averageLong(final T[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        return averageLong(a, fromIndex, toIndex, Fn.numToLong());
    }

    /**
     * Returns the average of the long values extracted from the elements in the provided array by the input {@code func} function as an {@code OptionalDouble}.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param func the function to extract a long value from each element
     * @return the average of the long values if the array is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#averageLong(Object[], ToLongFunction)
     */
    public static <T> OptionalDouble averageLong(final T[] a, final ToLongFunction<? super T> func) {
        if (N.isEmpty(a)) {
            return OptionalDouble.empty();
        }

        return averageLong(a, 0, a.length, func);
    }

    /**
     * Returns the average of the long values extracted from the elements in the specified range by the input {@code func} function as an {@code OptionalDouble}.
     * If the specified range is empty ({@code fromIndex == toIndex}, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param fromIndex the start index of the range, inclusive
     * @param toIndex the end index of the range, exclusive
     * @param func the function to extract a long value from each element
     * @return the average of the long values of the provided numbers in the specified range as an {@code OptionalDouble} if the ranger is not empty, otherwise an empty {@code OptionalDouble}
     * @throws IndexOutOfBoundsException if the range is invalid: ({@code fromIndex < 0 || fromIndex > toIndex || toIndex > a.length})
     * @see N#averageLong(Object[], int, int, ToLongFunction)
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
     * Returns the average of the long values of the provided numbers in the specified range as an {@code OptionalDouble}.
     * If the specified range is empty ({@code fromIndex == toIndex}, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the collection, which must be a subtype of Number
     * @param c the collection of elements to evaluate
     * @param fromIndex the start index of the range, inclusive
     * @param toIndex the end index of the range, exclusive
     * @return the average of the long values of the provided numbers in the specified range as an {@code OptionalDouble} if the ranger is not empty, otherwise an empty {@code OptionalDouble}
     * @throws IndexOutOfBoundsException if the range is invalid: ({@code fromIndex < 0 || fromIndex > toIndex || toIndex > a.length})
     * @see N#averageLong(Collection, int, int)
     */
    public static <T extends Number> OptionalDouble averageLong(final Collection<? extends T> c, final int fromIndex, final int toIndex)
            throws IndexOutOfBoundsException {
        return averageLong(c, fromIndex, toIndex, Fn.numToLong());
    }

    /**
     * Returns the average of the integer values extracted from the elements in the specified range by the input {@code func} function as an {@code OptionalDouble}.
     * If the specified range is empty ({@code fromIndex == toIndex}, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array
     * @param c the collection of elements to evaluate
     * @param fromIndex the start index of the range, inclusive
     * @param toIndex the end index of the range, exclusive
     * @param func the function to extract an integer value from each element
     * @return the average of the integer values of the provided numbers in the specified range as an {@code OptionalDouble} if the ranger is not empty, otherwise an empty {@code OptionalDouble}
     * @throws IndexOutOfBoundsException if the range is invalid: ({@code fromIndex < 0 || fromIndex > toIndex || toIndex > a.length})
     * @see N#averageLong(Object[], int, int, ToLongFunction)
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
     * Returns the average of the long values of the provided numbers as an {@code OptionalDouble}.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the iterable, which must be a subtype of Number
     * @param c the iterable of elements to evaluate
     * @return the average of the long values if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#averageLong(Iterable)
     */
    public static <T extends Number> OptionalDouble averageLong(final Iterable<? extends T> c) {
        return averageLong(c, Fn.numToLong());
    }

    /**
     * Returns the average of the long values extracted from the elements in the provided iterable by the input {@code func} function as an {@code OptionalDouble}.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param func the function to extract a long value from each element
     * @return the average of the long values if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#averageLong(Iterable, ToLongFunction)
     */
    public static <T> OptionalDouble averageLong(final Iterable<? extends T> c, final ToLongFunction<? super T> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.empty() : c.iterator();

        if (!iter.hasNext()) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(N.averageLong(c, func));
    }

    /**
     * Returns the average of the double values of the provided numbers as an {@code OptionalDouble}.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array, which must be a subtype of Number
     * @param a the array of elements to evaluate
     * @return the average of the double values if the array is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#averageDouble(Number[])
     */
    public static <T extends Number> OptionalDouble averageDouble(final T[] a) {
        return averageDouble(a, Fn.numToDouble());
    }

    /**
     * Returns the average of the double values of the provided numbers in the specified range as an {@code OptionalDouble}.
     * If the specified range is empty ({@code fromIndex == toIndex}, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array, which must be a subtype of Number
     * @param a the array of elements to evaluate
     * @param fromIndex the start index of the range, inclusive
     * @param toIndex the end index of the range, exclusive
     * @return the average of the double values of the provided numbers in the specified range as an {@code OptionalDouble} if the ranger is not empty, otherwise an empty {@code OptionalDouble}
     * @throws IndexOutOfBoundsException if the range is invalid: ({@code fromIndex < 0 || fromIndex > toIndex || toIndex > a.length})
     * @see N#averageDouble(Number[], int, int)
     */
    public static <T extends Number> OptionalDouble averageDouble(final T[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        return averageDouble(a, fromIndex, toIndex, Fn.numToDouble());
    }

    /**
     * Returns the average of the double values extracted from the elements in the provided array by the input {@code func} function as an {@code OptionalDouble}.
     * If the array is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param func the function to extract a double value from each element
     * @return the average of the double values if the array is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#averageDouble(Object[], ToDoubleFunction)
     */
    public static <T> OptionalDouble averageDouble(final T[] a, final ToDoubleFunction<? super T> func) {
        if (N.isEmpty(a)) {
            return OptionalDouble.empty();
        }

        return averageDouble(a, 0, a.length, func);
    }

    /**
     * Returns the average of the double values extracted from the elements in the specified range by the input {@code func} function as an {@code OptionalDouble}.
     * If the specified range is empty ({@code fromIndex == toIndex}, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array of elements to evaluate
     * @param fromIndex the start index of the range, inclusive
     * @param toIndex the end index of the range, exclusive
     * @param func the function to extract a double value from each element
     * @return the average of the double values of the provided numbers in the specified range as an {@code OptionalDouble} if the ranger is not empty, otherwise an empty {@code OptionalDouble}
     * @throws IndexOutOfBoundsException if the range is invalid: ({@code fromIndex < 0 || fromIndex > toIndex || toIndex > a.length})
     * @see N#averageDouble(Object[], int, int, ToDoubleFunction)
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
     * Returns the average of the double values of the provided numbers in the specified range as an {@code OptionalDouble}.
     * If the specified range is empty ({@code fromIndex == toIndex}, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the collection, which must be a subtype of Number
     * @param c the collection of elements to evaluate
     * @param fromIndex the start index of the range, inclusive
     * @param toIndex the end index of the range, exclusive
     * @return the average of the double values of the provided numbers in the specified range as an {@code OptionalDouble} if the ranger is not empty, otherwise an empty {@code OptionalDouble}
     * @throws IndexOutOfBoundsException if the range is invalid: ({@code fromIndex < 0 || fromIndex > toIndex || toIndex > a.length})
     * @see N#averageDouble(Collection, int, int)
     */
    public static <T extends Number> OptionalDouble averageDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex)
            throws IndexOutOfBoundsException {
        return averageDouble(c, fromIndex, toIndex, Fn.numToDouble());
    }

    /**
     * Returns the average of the double values extracted from the elements in the specified range by the input {@code func} function as an {@code OptionalDouble}.
     * If the specified range is empty ({@code fromIndex == toIndex}, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the collection
     * @param c the collection of elements to evaluate
     * @param fromIndex the start index of the range, inclusive
     * @param toIndex the end index of the range, exclusive
     * @param func the function to extract a double value from each element
     * @return the average of the double values of the provided numbers in the specified range as an {@code OptionalDouble} if the ranger is not empty, otherwise an empty {@code OptionalDouble}
     * @throws IndexOutOfBoundsException if the range is invalid: ({@code fromIndex < 0 || fromIndex > toIndex || toIndex > a.length})
     * @see N#averageDouble(Object[], int, int, ToDoubleFunction)
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
                if (idx < fromIndex) {
                    idx++;
                    continue;
                }

                summation.add(func.applyAsDouble(e));

                if (++idx >= toIndex) {
                    break;
                }
            }
        }

        return summation.average();
    }

    /**
     * Returns the average of the double values of the provided numbers as an {@code OptionalDouble}.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the iterable, which must be a subtype of Number
     * @param c the iterable of elements to evaluate
     * @return the average of the double values if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#averageDouble(Iterable)
     */
    public static <T extends Number> OptionalDouble averageDouble(final Iterable<? extends T> c) {
        return averageDouble(c, Fn.numToDouble());
    }

    /**
     * Returns the average of the double values extracted from the elements in the provided iterable by the input {@code func} function as an {@code OptionalDouble}.
     * If the iterable is {@code null} or empty, it returns an empty {@code OptionalDouble}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param func the function to extract a double value from each element
     * @return the average of the double values if the iterable is not {@code null} or empty, otherwise an empty {@code OptionalDouble}
     * @see N#averageDouble(Iterable, ToDoubleFunction)
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
     * Returns the average of the BigInteger values of the provided numbers as an {@code Optional<BigDecimal>}.
     * If the iterable is {@code null} or empty, it returns an empty {@code Optional<BigDecimal>}.
     *
     * @param c the iterable of elements to evaluate
     * @return the average of the BigInteger values if the iterable is not {@code null} or empty, otherwise an empty {@code Optional<BigDecimal>}
     * @see N#averageBigInteger(Iterable)
     */
    public static Optional<BigDecimal> averageBigInteger(final Iterable<? extends BigInteger> c) {
        return averageBigInteger(c, Fn.identity());
    }

    /**
     * Returns the average of the BigInteger values extracted from the elements in the provided iterable by the input {@code func} function as an {@code Optional<BigDecimal>}.
     * If the iterable is {@code null} or empty, it returns an empty {@code Optional<BigDecimal>}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param func the function to extract a BigInteger value from each element
     * @return the average of the BigInteger values if the iterable is not {@code null} or empty, otherwise an empty {@code Optional<BigDecimal>}
     * @see N#averageBigInteger(Iterable, Function)
     */
    public static <T> Optional<BigDecimal> averageBigInteger(final Iterable<? extends T> c, final Function<? super T, BigInteger> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.empty() : c.iterator();

        if (!iter.hasNext()) {
            return Optional.empty();
        }

        return Optional.of(N.averageBigInteger(c, func));
    }

    /**
     * Returns the average of the BigDecimal values of the provided numbers as an {@code Optional<BigDecimal>}.
     * If the iterable is {@code null} or empty, it returns an empty {@code Optional<BigDecimal>}.
     *
     * @param c the iterable of elements to evaluate
     * @return the average of the BigDecimal values if the iterable is not {@code null} or empty, otherwise an empty {@code Optional<BigDecimal>}
     * @see N#averageBigDecimal(Iterable)
     */
    public static Optional<BigDecimal> averageBigDecimal(final Iterable<? extends BigDecimal> c) {
        return averageBigDecimal(c, Fn.identity());
    }

    /**
     * Returns the average of the BigDecimal values extracted from the elements in the provided iterable by the input {@code func} function as an {@code Optional<BigDecimal>}.
     * If the iterable is {@code null} or empty, it returns an empty {@code Optional<BigDecimal>}.
     *
     * @param <T> the type of the elements in the iterable
     * @param c the iterable of elements to evaluate
     * @param func the function to extract a BigDecimal value from each element
     * @return the average of the BigDecimal values if the iterable is not {@code null} or empty, otherwise an empty {@code Optional<BigDecimal>}
     * @see N#averageBigDecimal(Iterable, Function)
     */
    public static <T> Optional<BigDecimal> averageBigDecimal(final Iterable<? extends T> c, final Function<? super T, BigDecimal> func) {
        final Iterator<? extends T> iter = c == null ? ObjIterator.empty() : c.iterator();

        if (!iter.hasNext()) {
            return Optional.empty();
        }

        return Optional.of(N.averageBigDecimal(c, func));
    }

    /**
     * Returns the index of the first occurrence of the specified value in the provided array as an {@code OptionalInt}.
     * If the array is {@code null} or doesn't contain the specified value, it returns an empty {@code OptionalInt}.
     *
     * @param a the array to search
     * @param valueToFind the value to find in the array
     * @return an {@code OptionalInt} containing the index of the first occurrence of the specified value if found, otherwise an empty {@code OptionalInt}
     * @see N#indexOf(Object[], Object)
     * @see Index#of(Object[], Object)
     */
    public static OptionalInt indexOf(final Object[] a, final Object valueToFind) {
        return Index.of(a, valueToFind);
    }

    /**
     * Returns the index of the first occurrence of the specified value in the provided collection as an {@code OptionalInt}.
     * If the collection is {@code null} or doesn't contain the specified value, it returns an empty {@code OptionalInt}.
     *
     * @param c the collection to search
     * @param valueToFind the value to find in the collection
     * @return an {@code OptionalInt} containing the index of the first occurrence of the specified value if found, otherwise an empty {@code OptionalInt}
     * @see N#indexOf(Collection, Object)
     * @see Index#of(Collection, Object)
     */
    public static OptionalInt indexOf(final Collection<?> c, final Object valueToFind) {
        return Index.of(c, valueToFind);
    }

    /**
     * Returns the index of the last occurrence of the specified value in the provided array as an {@code OptionalInt}.
     * If the array is {@code null} or doesn't contain the specified value, it returns an empty {@code OptionalInt}.
     *
     * @param a the array to search
     * @param valueToFind the value to find in the array
     * @return an {@code OptionalInt} containing the index of the last occurrence of the specified value if found, otherwise an empty {@code OptionalInt}
     * @see N#lastIndexOf(Object[], Object)
     * @see Index#last(Object[], Object)
     */
    public static OptionalInt lastIndexOf(final Object[] a, final Object valueToFind) {
        return Index.last(a, valueToFind);
    }

    /**
     * Returns the index of the last occurrence of the specified value in the provided collection as an {@code OptionalInt}.
     * If the collection is {@code null} or doesn't contain the specified value, it returns an empty {@code OptionalInt}.
     *
     * @param c the collection to search
     * @param valueToFind the value to find in the collection
     * @return an {@code OptionalInt} containing the index of the last occurrence of the specified value if found, otherwise an empty {@code OptionalInt}
     * @see N#lastIndexOf(Collection, Object)
     * @see Index#last(Collection, Object)
     */
    public static OptionalInt lastIndexOf(final Collection<?> c, final Object valueToFind) {
        return Index.last(c, valueToFind);
    }

    /**
     * Returns the first element in the provided array that satisfies the given {@code predicateForFirst},
     * or if no such element is found, returns the last element that satisfies the {@code predicateForLast}.
     * If the array is {@code null} or doesn't contain any element that satisfies the predicates, it returns an empty {@code Nullable}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array to search
     * @param predicateForFirst the predicate to test for the first element
     * @param predicateForLast the predicate to test for the last element
     * @return a {@code Nullable} containing the first element satisfying {@code predicateForFirst} if found,
     *         otherwise the last element satisfying {@code predicateForLast} if found, otherwise an empty {@code Nullable}
     * @see N#findFirst(Object[], Predicate)
     * @see N#findLast(Object[], Predicate)
     */
    public static <T> Nullable<T> findFirstOrLast(final T[] a, final Predicate<? super T> predicateForFirst, final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(a)) {
            return Nullable.empty();
        }

        final Nullable<T> res = N.findFirst(a, predicateForFirst);

        return res.isPresent() ? res : N.findLast(a, predicateForLast);
    }

    /**
     * Returns the first element in the provided collection that satisfies the given {@code predicateForFirst},
     * or if no such element is found, returns the last element that satisfies the {@code predicateForLast}.
     * If the collection is {@code null} or doesn't contain any element that satisfies the predicates, it returns an empty {@code Nullable}.
     *
     * @param <T> the type of the elements in the collection
     * @param c the collection to search
     * @param predicateForFirst the predicate to test for the first element
     * @param predicateForLast the predicate to test for the last element
     * @return a {@code Nullable} containing the first element satisfying {@code predicateForFirst} if found,
     *         otherwise the last element satisfying {@code predicateForLast} if found, otherwise an empty {@code Nullable}
     * @see N#findFirst(Iterable, Predicate)
     * @see N#findLast(Iterable, Predicate)
     */
    public static <T> Nullable<T> findFirstOrLast(final Collection<? extends T> c, final Predicate<? super T> predicateForFirst,
            final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(c)) {
            return Nullable.empty();
        }

        final Nullable<T> res = N.findFirst(c, predicateForFirst);

        return res.isPresent() ? res : N.findLast(c, predicateForLast);
    }

    /**
     * Returns the index of the first element in the provided array that satisfies the given {@code predicateForFirst},
     * or if no such element is found, returns the index of the last element that satisfies the {@code predicateForLast}.
     * If the array is {@code null} or doesn't contain any element that satisfies the predicates, it returns an empty {@code OptionalInt}.
     *
     * @param <T> the type of the elements in the array
     * @param a the array to search
     * @param predicateForFirst the predicate to test for the first element
     * @param predicateForLast the predicate to test for the last element
     * @return an {@code OptionalInt} containing the index of the first element satisfying {@code predicateForFirst} if found,
     *         otherwise the index of the last element satisfying {@code predicateForLast} if found, otherwise an empty {@code OptionalInt}
     * @see N#findFirstIndex(Object[], Predicate)
     * @see N#findLastIndex(Object[], Predicate)
     */
    public static <T> OptionalInt findFirstOrLastIndex(final T[] a, final Predicate<? super T> predicateForFirst, final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(a)) {
            return OptionalInt.empty();
        }

        final OptionalInt res = N.findFirstIndex(a, predicateForFirst);

        return res.isPresent() ? res : N.findLastIndex(a, predicateForLast);
    }

    /**
     * Returns the index of the first element in the provided collection that satisfies the given {@code predicateForFirst},
     * or if no such element is found, returns the index of the last element that satisfies the {@code predicateForLast}.
     * If the collection is {@code null} or doesn't contain any element that satisfies the predicates, it returns an empty {@code OptionalInt}.
     *
     * @param <T> the type of the elements in the collection
     * @param c the collection to search
     * @param predicateForFirst the predicate to test for the first element
     * @param predicateForLast the predicate to test for the last element
     * @return an {@code OptionalInt} containing the index of the first element satisfying {@code predicateForFirst} if found,
     *         otherwise the index of the last element satisfying {@code predicateForLast} if found, otherwise an empty {@code OptionalInt}
     * @see N#findFirstIndex(Collection, Predicate)
     * @see N#findLastIndex(Collection, Predicate)
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
     * Returns a pair of {@code Nullable} objects containing the first and last elements in the provided array that satisfy the given {@code predicate}.
     * If the array is {@code null} or doesn't contain any element that satisfies the predicate, it returns a pair of empty {@code Nullable} objects.
     *
     * @param <T> the type of the elements in the array
     * @param a the array to search
     * @param predicate the predicate to test for the first and last elements
     * @return a {@code Pair} containing a {@code Nullable} for the first element satisfying {@code predicate} if found and a {@code Nullable} for the last element satisfying {@code predicate} if found,
     *         otherwise a {@code Pair} of empty {@code Nullable} objects
     * @see #findFirstAndLast(Object[], Predicate, Predicate)
     * @see #findFirstOrLast(Object[], Predicate, Predicate)
     * @see N#findFirst(Object[], Predicate)
     * @see N#findLast(Object[], Predicate)
     */
    public static <T> Pair<Nullable<T>, Nullable<T>> findFirstAndLast(final T[] a, final Predicate<? super T> predicate) {
        return findFirstAndLast(a, predicate, predicate);
    }

    /**
     * Returns a pair of {@code Nullable} objects containing the first and last elements in the provided array that satisfy the given predicates.
     * If the array is {@code null} or doesn't contain any element that satisfies the predicates, it returns a pair of empty {@code Nullable} objects.
     *
     * @param <T> the type of the elements in the array
     * @param a the array to search
     * @param predicateForFirst the predicate to test for the first element
     * @param predicateForLast the predicate to test for the last element
     * @return a {@code Pair} containing a {@code Nullable} for the first element satisfying {@code predicateForFirst} if found and a {@code Nullable} for the last element satisfying {@code predicateForLast} if found,
     *         otherwise a {@code Pair} of empty {@code Nullable} objects
     * @see #findFirstAndLast(Object[], Predicate)
     * @see #findFirstOrLast(Object[], Predicate, Predicate)
     * @see N#findFirst(Object[], Predicate)
     * @see N#findLast(Object[], Predicate)
     */
    public static <T> Pair<Nullable<T>, Nullable<T>> findFirstAndLast(final T[] a, final Predicate<? super T> predicateForFirst,
            final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(a)) {
            return Pair.of(Nullable.empty(), Nullable.empty());
        }

        return Pair.of(N.findFirst(a, predicateForFirst), N.findLast(a, predicateForLast));
    }

    /**
     * Returns a pair of {@code Nullable} objects containing the first and last elements in the provided collection that satisfy the given {@code predicate}.
     * If the collection is {@code null} or doesn't contain any element that satisfies the predicate, it returns a pair of empty {@code Nullable} objects.
     *
     * @param <T> the type of the elements in the collection
     * @param c the collection to search
     * @param predicate the predicate to test for the first and last elements
     * @return a {@code Pair} containing a {@code Nullable} for the first element satisfying {@code predicate} if found and a {@code Nullable} for the last element satisfying {@code predicate} if found,
     *         otherwise a {@code Pair} of empty {@code Nullable} objects
     * @see #findFirstAndLast(Collection, Predicate, Predicate)
     * @see #findFirstOrLast(Collection, Predicate, Predicate)
     * @see N#findFirst(Iterable, Predicate)
     * @see N#findLast(Iterable, Predicate)
     */
    public static <T> Pair<Nullable<T>, Nullable<T>> findFirstAndLast(final Collection<? extends T> c, final Predicate<? super T> predicate) {
        return findFirstAndLast(c, predicate, predicate);
    }

    /**
     * Returns a pair of {@code Nullable} objects containing the first and last elements in the provided collection that satisfy the given predicates.
     * If the collection is {@code null} or doesn't contain any element that satisfies the predicates, it returns a pair of empty {@code Nullable} objects.
     *
     * @param <T> the type of the elements in the collection
     * @param c the collection to search
     * @param predicateForFirst the predicate to test for the first element
     * @param predicateForLast the predicate to test for the last element
     * @return a {@code Pair} containing a {@code Nullable} for the first element satisfying {@code predicateForFirst} if found and a {@code Nullable} for the last element satisfying {@code predicateForLast} if found,
     *         otherwise a {@code Pair} of empty {@code Nullable} objects
     * @see #findFirstAndLast(Collection, Predicate)
     * @see #findFirstOrLast(Collection, Predicate, Predicate)
     * @see N#findFirst(Iterable, Predicate)
     * @see N#findLast(Iterable, Predicate)
     */
    public static <T> Pair<Nullable<T>, Nullable<T>> findFirstAndLast(final Collection<? extends T> c, final Predicate<? super T> predicateForFirst,
            final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(c)) {
            return Pair.of(Nullable.empty(), Nullable.empty());
        }

        return Pair.of(N.findFirst(c, predicateForFirst), N.findLast(c, predicateForLast));
    }

    /**
     * Returns a pair of OptionalInt objects containing the indices of the first and last elements in the provided array that satisfy the given {@code predicate}.
     * If the array is {@code null} or doesn't contain any element that satisfies the predicate, it returns a pair of empty {@code OptionalInt} objects.
     *
     * @param <T> the type of the elements in the array
     * @param a the array to search
     * @param predicate the predicate to test for the first and last elements
     * @return a {@code Pair} containing an {@code OptionalInt} for the index of the first element satisfying {@code predicate} if found and an {@code OptionalInt} for the index of the last element satisfying {@code predicate} if found,
     *         otherwise a {@code Pair} of empty {@code OptionalInt} objects
     * @see #findFirstAndLastIndex(Object[], Predicate, Predicate)
     * @see #findFirstOrLastIndex(Object[], Predicate, Predicate)
     * @see N#findFirstIndex(Object[], Predicate)
     * @see N#findLastIndex(Object[], Predicate)
     */
    public static <T> Pair<OptionalInt, OptionalInt> findFirstAndLastIndex(final T[] a, final Predicate<? super T> predicate) {
        return findFirstAndLastIndex(a, predicate, predicate);
    }

    /**
     * Returns a pair of OptionalInt objects containing the indices of the first and last elements in the provided array that satisfy the given predicates.
     * If the array is {@code null} or doesn't contain any element that satisfies the predicates, it returns a pair of empty {@code OptionalInt} objects.
     *
     * @param <T> the type of the elements in the array
     * @param a the array to search
     * @param predicateForFirst the predicate to test for the first element
     * @param predicateForLast the predicate to test for the last element
     * @return a {@code Pair} containing an {@code OptionalInt} for the index of the first element satisfying {@code predicateForFirst} if found and an {@code OptionalInt} for the index of the last element satisfying {@code predicateForLast} if found,
     *         otherwise a {@code Pair} of empty {@code OptionalInt} objects
     * @see #findFirstAndLastIndex(Object[], Predicate)
     * @see #findFirstOrLastIndex(Object[], Predicate, Predicate)
     * @see N#findFirstIndex(Object[], Predicate)
     * @see N#findLastIndex(Object[], Predicate)
     */
    public static <T> Pair<OptionalInt, OptionalInt> findFirstAndLastIndex(final T[] a, final Predicate<? super T> predicateForFirst,
            final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(a)) {
            return Pair.of(OptionalInt.empty(), OptionalInt.empty());
        }

        return Pair.of(N.findFirstIndex(a, predicateForFirst), N.findLastIndex(a, predicateForLast));
    }

    /**
     * Returns a pair of OptionalInt objects containing the indices of the first and last elements in the provided collection that satisfy the given {@code predicate}.
     * If the collection is {@code null} or doesn't contain any element that satisfies the predicate, it returns a pair of empty {@code OptionalInt} objects.
     *
     * @param <T> the type of the elements in the collection
     * @param c the collection to search
     * @param predicate the predicate to test for the first and last elements
     * @return a {@code Pair} containing an {@code OptionalInt} for the index of the first element satisfying {@code predicate} if found and an {@code OptionalInt} for the index of the last element satisfying {@code predicate} if found,
     *         otherwise a {@code Pair} of empty {@code OptionalInt} objects
     * @see #findFirstAndLastIndex(Collection, Predicate, Predicate)
     * @see #findFirstOrLastIndex(Collection, Predicate, Predicate)
     * @see N#findFirstIndex(Collection, Predicate)
     * @see N#findLastIndex(Collection, Predicate)
     */
    public static <T> Pair<OptionalInt, OptionalInt> findFirstAndLastIndex(final Collection<? extends T> c, final Predicate<? super T> predicate) {
        return findFirstAndLastIndex(c, predicate, predicate);
    }

    /**
     * Returns a pair of OptionalInt objects containing the indices of the first and last elements in the provided collection that satisfy the given predicates.
     * If the collection is {@code null} or doesn't contain any element that satisfies the predicates, it returns a pair of empty {@code OptionalInt} objects.
     *
     * @param <T> the type of the elements in the collection
     * @param c the collection to search
     * @param predicateForFirst the predicate to test for the first element
     * @param predicateForLast the predicate to test for the last element
     * @return a {@code Pair} containing an {@code OptionalInt} for the index of the first element satisfying {@code predicateForFirst} if found and an {@code OptionalInt} for the index of the last element satisfying {@code predicateForLast} if found,
     *         otherwise a {@code Pair} of empty {@code OptionalInt} objects
     * @see #findFirstAndLastIndex(Collection, Predicate)
     * @see #findFirstOrLastIndex(Collection, Predicate, Predicate)
     * @see N#findFirstIndex(Collection, Predicate)
     * @see N#findLastIndex(Collection, Predicate)
     */
    public static <T> Pair<OptionalInt, OptionalInt> findFirstAndLastIndex(final Collection<? extends T> c, final Predicate<? super T> predicateForFirst,
            final Predicate<? super T> predicateForLast) {
        if (N.isEmpty(c)) {
            return Pair.of(OptionalInt.empty(), OptionalInt.empty());
        }

        return Pair.of(N.findFirstIndex(c, predicateForFirst), N.findLastIndex(c, predicateForLast));
    }

    /**
     * Fills the specified Object array with the values provided by the specified supplier.
     *
     * @param a the Object array to be filled
     * @param supplier provider of the value to fill the array with
     * @see Arrays#fill(Object[], Object)
     * @see N#setAll(Object[], IntFunction)
     * @see N#replaceAll(Object[], UnaryOperator) 
     * @see N#fill(Object[], Object)
     * @see N#fill(Object[], int, int, Object)
     * @see Fn#s(com.landawn.abacus.util.function.Supplier)
     * @see Fn#s(Object, com.landawn.abacus.util.function.Function)
     * @see Fn.Suppliers#of(com.landawn.abacus.util.function.Supplier)
     * @see Fn.Suppliers#of(Object, com.landawn.abacus.util.function.Function)
     */
    @Beta
    public static <T> void fill(final T[] a, final Supplier<? extends T> supplier) {
        if (N.isEmpty(a)) {
            return;
        }

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = supplier.get();
        }
    }

    /**
     * Fills the specified Object array with the values provided by the specified supplier from the specified fromIndex (inclusive) to the specified toIndex (exclusive).
     *
     * @param a the Object array to be filled
     * @param fromIndex the index to start filling (inclusive)
     * @param toIndex the index to stop filling (exclusive)
     * @param supplier provider of the value to fill the array with
     * @throws IndexOutOfBoundsException if the fromIndex or toIndex is out of bounds
     * @see Arrays#fill(Object[], int, int, Object)
     * @see N#fill(Object[], Object)
     * @see N#fill(Object[], int, int, Object)
     * @see Fn#s(com.landawn.abacus.util.function.Supplier)
     * @see Fn#s(Object, com.landawn.abacus.util.function.Function)
     * @see Fn.Suppliers#of(com.landawn.abacus.util.function.Supplier)
     * @see Fn.Suppliers#of(Object, com.landawn.abacus.util.function.Function)
     */
    @Beta
    public static <T> void fill(final T[] a, final int fromIndex, final int toIndex, final Supplier<? extends T> supplier) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return;
        }

        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = supplier.get();
        }
    }

    /**
     * Fills the specified list with values provided by the specified supplier.
     *
     * @param <T> the type of elements in the list
     * @param list the list to be filled
     * @param supplier provider of the value to fill the list with
     * @throws IllegalArgumentException if the specified list is null
     * @see N#fill(List, Object)
     * @see N#fill(List, int, int, Object)
     * @see N#setAll(List, java.util.function.IntFunction)
     * @see N#replaceAll(List, java.util.function.UnaryOperator) 
     * @see N#padLeft(List, int, Object)
     * @see N#padRight(Collection, int, Object)
     * @see Fn#s(com.landawn.abacus.util.function.Supplier)
     * @see Fn#s(Object, com.landawn.abacus.util.function.Function)
     * @see Fn.Suppliers#of(com.landawn.abacus.util.function.Supplier)
     * @see Fn.Suppliers#of(Object, com.landawn.abacus.util.function.Function)
     */
    @Beta
    public static <T> void fill(final List<? super T> list, final Supplier<? extends T> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(list, cs.list);

        fill(list, 0, list.size(), supplier);
    }

    /**
     * Fills the specified list with the specified with values provided by the specified supplier from the specified start index to the specified end index.
     * The list will be extended automatically if the size of the list is less than the specified toIndex.
     *
     * @param <T> the type of elements in the list
     * @param list the list to be filled
     * @param fromIndex the starting index (inclusive) to begin filling
     * @param toIndex the ending index (exclusive) to stop filling
     * @param supplier provider of the value to fill the list with
     * @throws IllegalArgumentException if the specified list is null
     * @throws IndexOutOfBoundsException if the specified indices are out of range
     * @see N#fill(List, Object)
     * @see N#fill(List, int, int, Object)
     * @see N#setAll(List, java.util.function.IntFunction)
     * @see N#replaceAll(List, java.util.function.UnaryOperator)
     * @see N#padLeft(List, int, Object)
     * @see N#padRight(Collection, int, Object)
     * @see Fn#s(com.landawn.abacus.util.function.Supplier)
     * @see Fn#s(Object, com.landawn.abacus.util.function.Function)
     * @see Fn.Suppliers#of(com.landawn.abacus.util.function.Supplier)
     * @see Fn.Suppliers#of(Object, com.landawn.abacus.util.function.Function)
     */
    @Beta
    public static <T> void fill(final List<? super T> list, final int fromIndex, final int toIndex, final Supplier<? extends T> supplier)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkArgNotNull(list, cs.list);
        N.checkFromToIndex(fromIndex, toIndex, Integer.MAX_VALUE);

        final int size = list.size();

        if (size < toIndex) {
            if (fromIndex < size) {
                for (int i = fromIndex; i < size; i++) {
                    list.set(i, supplier.get());
                }
            } else {
                for (int i = size; i < fromIndex; i++) {
                    list.add(null);
                }
            }

            for (int i = 0, len = toIndex - list.size(); i < len; i++) {
                list.add(supplier.get());
            }
        } else {
            if (toIndex - fromIndex < N.FILL_THRESHOLD || list instanceof RandomAccess) {
                for (int i = fromIndex; i < toIndex; i++) {
                    list.set(i, supplier.get());
                }
            } else {
                final ListIterator<? super T> itr = list.listIterator(fromIndex);

                for (int i = fromIndex; i < toIndex; i++) {
                    itr.next();

                    itr.set(supplier.get());
                }
            }
        }
    }

    /**
     * Copies all the elements from the source list into the destination list.
     * After the operation, the index of each copied element in the destination list
     * will be identical to its index in the source list. The destination list must
     * be at least as long as the source list. If it is longer, the remaining elements
     * in the destination list are unaffected.
     *
     * This method runs in linear time.
     *
     * @param <T> the type of elements in the lists
     * @param src the source list from which elements are to be copied
     * @param dest the destination list to which elements are to be copied
     * @throws IndexOutOfBoundsException if the destination list is too small to contain the entire source list
     * @throws UnsupportedOperationException if the destination list's list-iterator does not support the set operation
     * @see #copy(List, int, List, int, int)
     * @see java.util.Collections#copy(List, List)
     * @see N#copy(Object[], int, Object[], int, int) 
     */
    // Moved from Class CommonUtil/N to Iterables to avoid ambiguity with CommonUtil.copy(Object, Collection<String>).
    public static <T> void copy(final List<? extends T> src, final List<? super T> dest) {
        if (CommonUtil.isEmpty(src)) {
            return;
        }

        if (src.size() > dest.size()) {
            throw new IndexOutOfBoundsException("Source does not fit in dest");
        }

        Collections.copy(dest, src);
    }

    /**
     * Copies a portion of one list into another. The portion to be copied begins at the index srcPos in the source list and spans length elements.
     * The elements are copied into the destination list starting at position destPos. Both source and destination positions are zero-based.
     *
     * @param <T> the type of elements in the lists
     * @param src the source list from which to copy elements
     * @param srcPos the starting position in the source list
     * @param dest the destination list into which to copy elements
     * @param destPos the starting position in the destination list
     * @param length the number of elements to be copied
     * @throws IndexOutOfBoundsException if copying would cause access of data outside list bounds
     * @see #copy(List, List)
     * @see N#copy(Object[], int, Object[], int, int) 
     * @see Collections#copy(List, List)
     */
    // Moved from Class CommonUtil/N to Iterables to avoid ambiguity with CommonUtil.copy(Object, Collection<String>).
    public static <T> void copy(final List<? extends T> src, final int srcPos, final List<? super T> dest, final int destPos, final int length)
            throws IndexOutOfBoundsException {
        CommonUtil.checkFromToIndex(srcPos, srcPos + length, CommonUtil.size(src));
        CommonUtil.checkFromToIndex(destPos, destPos + length, CommonUtil.size(dest));

        if (CommonUtil.isEmpty(src) && srcPos == 0 && length == 0) {
            return;
        }

        if (src instanceof RandomAccess && dest instanceof RandomAccess) {
            for (int i = 0; i < length; i++) {
                dest.set(destPos + i, src.get(srcPos + i));
            }
        } else {
            final ListIterator<? extends T> srcIterator = src.listIterator();
            final ListIterator<? super T> destIterator = dest.listIterator();

            int idx = 0;
            while (idx < srcPos) {
                srcIterator.next();
                idx++;
            }

            idx = 0;
            while (idx < destPos) {
                destIterator.next();
                idx++;
            }

            for (int i = 0; i < length; i++) {
                destIterator.next();
                destIterator.set(srcIterator.next());
            }
        }
    }

    /**
     * <p>Copied from Google Guava under Apache License v2.0 and may be modified.</p>
     *
     * Returns a reversed view of the specified list. For example, {@code
     * reverse(Arrays.asList(1, 2, 3))} returns a list containing {@code 3, 2, 1}. The returned
     * list is backed by this list, so changes in the returned list are reflected in this list, and
     * vice versa. The returned list supports all the optional list operations supported by this
     * list.
     *
     * <p>The returned list is random-access if the specified list is random access.</p>
     *
     * @param <T> the type of elements in the list
     * @param list the list to be reversed
     * @return a reversed view of the specified list
     * @see N#reverse(List)
     * @see N#reverse(Collection)
     * @see N#reverseToList(Collection)
     */
    public static <T> List<T> reverse(final List<T> list) {
        if (list instanceof ImmutableList) {
            // Avoid nullness warnings.
            final List<?> reversed = ((ImmutableList<?>) list).reverse();
            return (List<T>) reversed;
        } else if (list instanceof ReverseList) {
            return ((ReverseList<T>) list).getForwardList();
        } else if (list instanceof RandomAccess) {
            return new RandomAccessReverseList<>(list);
        } else {
            return new ReverseList<>(list);
        }
    }

    // Copied from Google Guava under Apache License v2.0 and may be modified.
    private static sealed class ReverseList<T> extends AbstractList<T> permits RandomAccessReverseList {
        private final List<T> forwardList;

        ReverseList(final List<T> forwardList) {
            this.forwardList = N.checkArgNotNull(forwardList);
        }

        List<T> getForwardList() {
            return forwardList;
        }

        private int reverseIndex(final int index) {
            final int size = size();
            N.checkElementIndex(index, size);
            return (size - 1) - index;
        }

        private int reversePosition(final int index) {
            final int size = size();
            N.checkPositionIndex(index, size);
            return size - index;
        }

        @Override
        public void add(final int index, final T element) {
            forwardList.add(reversePosition(index), element);
        }

        @Override
        public void clear() {
            forwardList.clear();
        }

        @Override
        public T remove(final int index) {
            return forwardList.remove(reverseIndex(index));
        }

        @Override
        protected void removeRange(final int fromIndex, final int toIndex) {
            subList(fromIndex, toIndex).clear();
        }

        @Override
        public T set(final int index, final T element) {
            return forwardList.set(reverseIndex(index), element);
        }

        @Override
        public T get(final int index) {
            return forwardList.get(reverseIndex(index));
        }

        @Override
        public int size() {
            return forwardList.size();
        }

        @Override
        public List<T> subList(final int fromIndex, final int toIndex) {
            N.checkFromToIndex(fromIndex, toIndex, size());
            return reverse(forwardList.subList(reversePosition(toIndex), reversePosition(fromIndex)));
        }

        @Override
        public Iterator<T> iterator() {
            return listIterator();
        }

        @Override
        public ListIterator<T> listIterator(final int index) {
            final int start = reversePosition(index);
            final ListIterator<T> forwardIterator = forwardList.listIterator(start);

            return new ListIterator<>() {
                boolean canRemoveOrSet;

                @Override
                public void add(final T e) {
                    forwardIterator.add(e);
                    forwardIterator.previous();
                    canRemoveOrSet = false;
                }

                @Override
                public boolean hasNext() {
                    return forwardIterator.hasPrevious();
                }

                @Override
                public boolean hasPrevious() {
                    return forwardIterator.hasNext();
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    canRemoveOrSet = true;
                    return forwardIterator.previous();
                }

                @Override
                public int nextIndex() {
                    return reversePosition(forwardIterator.nextIndex());
                }

                @Override
                public T previous() {
                    if (!hasPrevious()) {
                        throw new NoSuchElementException();
                    }

                    canRemoveOrSet = true;

                    return forwardIterator.next();
                }

                @Override
                public int previousIndex() {
                    return nextIndex() - 1;
                }

                @Override
                public void remove() {
                    checkRemove(canRemoveOrSet);
                    forwardIterator.remove();
                    canRemoveOrSet = false;
                }

                @Override
                public void set(final T e) {
                    N.checkState(canRemoveOrSet);
                    forwardIterator.set(e);
                }
            };
        }
    }

    // Copied from Google Guava under Apache License v2.0 and may be modified.
    private static final class RandomAccessReverseList<T> extends ReverseList<T> implements RandomAccess {
        RandomAccessReverseList(final List<T> forwardList) {
            super(forwardList);
        }
    }

    static void checkRemove(final boolean canRemove) {
        N.checkState(canRemove, "no calls to next() since the last call to remove()");
    }

    public abstract static class SetView<E> extends ImmutableSet<E> {
        SetView(final Set<? extends E> set) {
            super(set);
        }

        /**
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
            tmp = N.isEmpty(set2) ? N.emptySet() : set2;
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
            tmp = N.emptySet();
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
     * all elements that are contained in {@code set1} but not in {@code set2}.
     *
     * <p>Example:
     * <pre>
     * Set&lt;String&gt; set1 = new HashSet&lt;&gt;(Arrays.asList("a", "b", "c", "d"));
     * Set&lt;String&gt; set2 = new HashSet&lt;&gt;(Arrays.asList("b", "d", "e"));
     * 
     * SetView&lt;String&gt; difference = Iterables.difference(set1, set2);
     * // difference contains: "a", "c"
     * 
     * // The view can be copied to a new set if needed
     * Set&lt;String&gt; diffCopy = difference.copyInto(new HashSet&lt;&gt;());
     * </pre>
     *
     * <p>The iteration order of the returned set matches that of {@code set1}.
     * Elements in {@code set2} that are not present in {@code set1} are simply ignored.
     * The returned view is backed by {@code set1}, so changes in {@code set1} may be reflected in the view.
     *
     * <p>Results are undefined if {@code set1} and {@code set2} are sets based on different
     * equivalence relations (as {@link HashSet}, {@link TreeSet}, and the keySet of an
     * {@code IdentityHashMap} all are).
     *
     * @param <E> the type of elements in the returned set
     * @param set1 the base set whose elements may be included in the result
     * @param set2 the set containing elements to be excluded from the result
     * @return an unmodifiable view of the elements in {@code set1} but not in {@code set2}
     * @see N#difference(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Iterable)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    public static <E> SetView<E> difference(final Set<E> set1, final Set<?> set2) {
        // N.checkArgNotNull(set1, "set1");
        // N.checkArgNotNull(set2, "set2");

        Set<E> tmp = null;

        if (N.isEmpty(set1)) {
            tmp = N.emptySet();
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
                    //noinspection SuspiciousMethodCalls
                    return set2.containsAll(set1);
                }
            };
        }

        return new SetView<>(tmp) {
        };
    }

    /**
     * Returns an unmodifiable <b>view</b> of the symmetric difference of two sets. The returned set contains
     * all elements that are contained in either {@code set1} or {@code set2} but not in both.
     * 
     * <p>Example:
     * <pre>
     * Set&lt;String&gt; set1 = new HashSet&lt;&gt;(Arrays.asList("a", "b", "c"));
     * Set&lt;String&gt; set2 = new HashSet&lt;&gt;(Arrays.asList("b", "c", "d"));
     * 
     * SetView&lt;String&gt; symDiff = Iterables.symmetricDifference(set1, set2);
     * // symDiff contains: "a", "d"
     * 
     * // The view can be copied to a new set if needed
     * Set&lt;String&gt; symDiffCopy = symDiff.copyInto(new HashSet&lt;&gt;());
     * </pre>
     * 
     * <p>The iteration order of the returned set is undefined.
     * The returned view is backed by the input sets, so changes in the original sets may be reflected in the view.
     * 
     * <p>Results are undefined if {@code set1} and {@code set2} are sets based on different
     * equivalence relations (as {@link HashSet}, {@link TreeSet}, and the keySet of an
     * {@code IdentityHashMap} all are).
     * 
     * @param <E> the type of elements in the returned set
     * @param set1 the first set to find elements not present in the other set
     * @param set2 the second set to find elements not present in the other set
     * @return an unmodifiable view of elements present in either {@code set1} or {@code set2} but not in both
     * @throws IllegalArgumentException if either input set is null
     * @see N#symmetricDifference(Collection, Collection) 
     * @see N#symmetricDifference(int[], int[])
     * @see N#difference(Collection, Collection)
     * @see #difference(Set, Set)
     * @see #intersection(Set, Set)
     * @see #union(Set, Set)
     */
    public static <E> SetView<E> symmetricDifference(final Set<? extends E> set1, final Set<? extends E> set2) throws IllegalArgumentException {
        // N.checkArgNotNull(set1, "set1");
        // N.checkArgNotNull(set2, "set2");

        Set<? extends E> tmp = null;

        if (N.isEmpty(set1)) {
            tmp = N.isEmpty(set2) ? N.emptySet() : set2;
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
     * Returns a subset of the provided NavigableSet that falls within the specified range.
     * The subset includes all elements in the NavigableSet that are within the range defined by the lower and upper endpoints of the Range object.
     * The returned NavigableSet is a view of the original set, meaning changes in the returned set are reflected in the original set and vice versa.
     * The iteration order of the returned set matches that of the original set.
     *
     * @param <K> the type of elements in the set, which must extend Comparable
     * @param set the original NavigableSet from which to derive the subset
     * @param range the Range object that defines the lower and upper bounds of the subset
     * @return a NavigableSet that includes all elements within the specified range
     * @throws IllegalArgumentException if the set is sorted by a non-natural ordering and the range endpoints are not mutually comparable
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
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     * 
     *
     * Returns the set of all possible subsets of {@code set}. For example,
     * {@code powerSet(ImmutableSet.of(1, 2))} returns the set {@code {{},
     * {1}, {2}, {1, 2}}}.
     *
     * <p>Elements appear in these subsets in the same iteration order as they
     * appeared in the input set. The order in which these subsets appear in the
     * outer set is undefined. Note that the power set of the empty set is not the
     * empty set, but an one-element set containing the empty set.
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
     * @return the sets of all possible subsets of {@code set}
     * @throws IllegalArgumentException if {@code set} has more than 30 unique
     *     elements (causing the power set size to exceed the {@code int} range)
     * @see <a href="http://en.wikipedia.org/wiki/Power_set">Power set article at
     *      Wikipedia</a>
     */
    public static <E> Set<Set<E>> powerSet(final Set<E> set) {
        return new PowerSet<>(N.nullToEmpty(set));
    }

    /**
     * Generates a rollup (a list of cumulative subsets) of the given collection.
     * Each subset is a list that includes the elements of the original collection from the start to a certain index.
     * The rollup starts with an empty list, and each subsequent list in the rollup includes one more element from the collection.
     * For example, given a collection [a, b, c], the rollup would be [[], [a], [a, b], [a, b, c]].
     *
     * @param <T> the type of elements in the collection
     * @param c the original collection from which to generate the rollup
     * @return a list of lists representing the rollup of the original collection
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
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     * 
     *
     * Returns a {@link Collection} of all the permutations of the specified
     * {@link Collection}.
     *
     * <p><i>Notes:</i> This is an implementation of the Plain Changes algorithm
     * for permutation generation, described in Knuth's "The Art of Computer
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
     * @throws IllegalArgumentException if the specified collection is {@code null} or has any
     *     {@code null} elements.
     */
    public static <E> Collection<List<E>> permutations(final Collection<E> elements) throws IllegalArgumentException {
        N.checkArgNotNull(elements, cs.collection);

        return new PermutationCollection<>(elements);
    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
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
     * @throws IllegalArgumentException if the specified collection is {@code null} or has any
     *     {@code null} elements.
     */
    public static <E extends Comparable<? super E>> Collection<List<E>> orderedPermutations(final Collection<E> elements) throws IllegalArgumentException {
        N.checkArgNotNull(elements, cs.collection);

        return orderedPermutations(N.nullToEmpty(elements), N.NATURAL_COMPARATOR);
    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
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
     * <p>Elements that compare equal are considered equal, and no new permutations
     * are created by swapping them.
     *
     * <p>An empty iterable has only one permutation, which is an empty list.
     *
     * @param <E>
     * @param elements the original iterable whose elements have to be permuted.
     * @param comparator a comparator for the iterable's elements.
     * @return an immutable {@link Collection} containing all the different
     *     permutations of the original iterable.
     * @throws IllegalArgumentException if the specified collection is {@code null} or has any
     *     {@code null} elements.
     */
    public static <E> Collection<List<E>> orderedPermutations(final Collection<E> elements, final Comparator<? super E> comparator)
            throws IllegalArgumentException {
        N.checkArgNotNull(elements, cs.collection);

        return new OrderedPermutationCollection<>(N.nullToEmpty(elements), comparator);
    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
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
     * @throws IllegalArgumentException if the size of the cartesian product is greater than {@link Integer#MAX_VALUE}
     */
    @SafeVarargs
    public static <E> List<List<E>> cartesianProduct(final Collection<? extends E>... cs) {
        return cartesianProduct(Array.asList(cs));
    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
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
     * @throws IllegalArgumentException if the size of the cartesian product is greater than {@link Integer#MAX_VALUE}
     */
    public static <E> List<List<E>> cartesianProduct(final Collection<? extends Collection<? extends E>> cs) {
        return new CartesianList<>(N.nullToEmpty(cs));
    }

    /**
     * Returns {@code true} if the second list is a permutation of the first.
     *
     * @param a
     * @param b
     * @return {@code true}, if is permutations
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
         * @return {@code true}, if is empty
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
            if (obj instanceof final Set<?> set) {
                //noinspection SuspiciousMethodCalls
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
            if (obj instanceof final PowerSet<?> that) {
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

        @Override
        public String toString() {
            return "powerSet(" + inputSet + ")";
        }

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
         * Instantiates a new subset.
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
            @SuppressWarnings("SuspiciousMethodCalls")
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
            inputList = ImmutableList.copyOf(input);
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
         * @return {@code true}, if is empty
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
         * @return {@code true}, if is empty
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
         * @return {@code true}, if is positive int
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
        public List<E> get(final int index) throws IndexOutOfBoundsException {
            N.checkElementIndex(index, size());

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
            if (!(obj instanceof final Collection<?> c) || (c.size() != axes.length)) {
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
    @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
    static final class Slice<T> extends ImmutableCollection<T> { //NOSONAR

        /** The start index. */
        private final int fromIndex;

        /** The to index. */
        private final int toIndex;

        /**
         * Instantiates a new subcollection.
         *
         * @param a
         * @param fromIndex
         * @param toIndex
         */
        Slice(final T[] a, final int fromIndex, final int toIndex) {
            this(Array.asList(a), fromIndex, toIndex);
        }

        /**
         * Instantiates a new subcollection.
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
         * Instantiates a new subcollection.
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
         * @return {@code true}, if is empty
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
                return ObjIterator.empty();
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
        final Iterator<T> iter = arg == null ? ObjIterator.empty() : arg.iterator();
        final boolean isNullOrEmpty = arg == null || (arg instanceof Collection ? ((Collection<T>) arg).size() == 0 : !iter.hasNext());

        if (isNullOrEmpty) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' cannot be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return iter;
    }
}

/*
 * Copyright (c) 2017, Haiyang Li.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.Stream;

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
 * user tries to reverse a {@code null} or empty String. the input String will be
 * returned. But exception will be thrown if try to add element to a {@code null} Object array or collection.
 * <br />
 * <br />
 * An empty String/Array/Collection/Map/Iterator/Iterable/InputStream/Reader will always be a preferred choice than a {@code null} for the return value of a method.
 * <br />
 * <p>
 * This is a utility class mostly for {@code Iterator}.
 * </p>
 *
 * <p>
 * The methods in this class should only read the input {@code Collection/Array/Iterator} parameters, not modify them.
 * </p>
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Index
 * @see com.landawn.abacus.util.Median
 * @see com.landawn.abacus.util.Maps
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.ObjIterator
 * @see com.landawn.abacus.util.Enumerations
 */
public final class Iterators {

    private static final Logger logger = LoggerFactory.getLogger(Iterators.class);

    private Iterators() {
        // Utility class.
    }

    /**
     * Retrieves the element at the specified position in the given iterator.
     * The method will advance the iterator to the specified index and return the element at that position wrapped in a Nullable.
     * If the index is out of bounds (greater than the number of elements in the iterator), a Nullable.empty() is returned.
     *
     * @param <T> The type of elements in the iterator.
     * @param iter The iterator from which to retrieve the element.
     * @param index The position in the iterator of the element to be returned. Indexing starts from 0.
     * @return A Nullable containing the element at the specified position in the iterator, or Nullable.empty() if the index is out of bounds.
     * @throws IllegalArgumentException if the index is negative.
     */
    public static <T> Nullable<T> get(final Iterator<? extends T> iter, long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        if (iter == null) {
            return Nullable.empty();
        }

        while (iter.hasNext()) {
            if (index-- == 0) {
                return Nullable.<T> of(iter.next());
            } else {
                iter.next();
            }
        }

        return Nullable.empty();
    }

    /**
     * Counts the occurrences of a specific value in the given iterator.
     * This method can be used to find the frequency of a particular value in the iterator.
     *
     * @param iter The iterator to be searched.
     * @param valueToFind The value to count occurrences of.
     * @return The number of occurrences of the value in the iterator.
     * @see N#occurrencesOf(Iterator, Object)
     */
    public static long occurrencesOf(final Iterator<?> iter, final Object valueToFind) {
        if (iter == null) {
            return 0;
        }

        long occurrences = 0;

        if (valueToFind == null) {
            while (iter.hasNext()) {
                if (iter.next() == null) {
                    occurrences++;
                }
            }
        } else {
            while (iter.hasNext()) {
                if (N.equals(iter.next(), valueToFind)) {
                    occurrences++;
                }
            }
        }

        return occurrences;
    }

    /**
     * Counts the number of elements in the given iterator.
     *
     * @param iter The iterator to be counted.
     * @return The number of elements in the iterator.
     * @see N#count(Iterator)
     * @see #count(Iterator, Predicate)
     */
    public static long count(final Iterator<?> iter) {
        if (iter == null) {
            return 0;
        }

        long res = 0;

        while (iter.hasNext()) {
            iter.next();
            res++;
        }

        return res;
    }

    /**
     * Counts the number of elements in the given iterator that match the provided predicate.
     *
     * @param <T> The type of elements in the iterator.
     * @param iter The iterator to be searched.
     * @param predicate The predicate to apply to each element in the iterator.
     * @return The number of elements in the iterator that match the provided predicate.
     * @throws IllegalArgumentException if the predicate is {@code null}.
     * @see N#count(Iterator, Predicate)
     */
    public static <T> long count(final Iterator<? extends T> iter, final Predicate<? super T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate); //NOSONAR

        if (iter == null) {
            return 0;
        }

        long res = 0;

        while (iter.hasNext()) {
            if (predicate.test(iter.next())) {
                res++;
            }
        }

        return res;
    }

    /**
     * Returns the index of the first occurrence of the specified value in the given iterator.
     * This method starts searching from the beginning of the iterator.
     *
     * @param iter The iterator to be searched.
     * @param valueToFind The value to find in the iterator.
     * @return The index of the first occurrence of the specified value in the iterator, or -1 if the value is not found.
     */
    public static long indexOf(final Iterator<?> iter, final Object valueToFind) {
        return indexOf(iter, valueToFind, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified value in the given iterator,
     * starting the search from the specified index.
     *
     * @param iter The iterator to be searched.
     * @param valueToFind The value to find in the iterator.
     * @param fromIndex The index to start the search from.
     * @return The index of the first occurrence of the specified value in the iterator, or -1 if the value is not found.
     */
    public static long indexOf(final Iterator<?> iter, final Object valueToFind, final long fromIndex) {
        if (iter == null) {
            return N.INDEX_NOT_FOUND;
        }

        long index = 0;

        if (fromIndex > 0) {
            while (index < fromIndex && iter.hasNext()) {
                iter.next();
                index++;
            }
        }

        while (iter.hasNext()) {
            if (N.equals(iter.hasNext(), valueToFind)) {
                return index;
            }

            index++;
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Note: Copied from Google Guava under the Apache License 2.0.
     *
     * <br />
     * <br />
     *
     * Determines whether two iterators contain equal elements in the same order. More specifically,
     * this method returns {@code true} if {@code iterator1} and {@code iterator2} contain the same
     * number of elements and every element of {@code iterator1} is equal to the corresponding element
     * of {@code iterator2}.
     *
     * <p>Note that this will modify the supplied iterators, since they will have been advanced some
     * number of elements forward.
     */
    public static boolean elementsEqual(final Iterator<?> iterator1, final Iterator<?> iterator2) {
        while (iterator1.hasNext()) {
            if (!iterator2.hasNext() || !N.equals(iterator1.next(), iterator2.next())) {
                return false;
            }
        }

        return !iterator2.hasNext();
    }

    /**
     *
     *
     * @param <T>
     * @param e
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> ObjIterator<T> repeat(final T e, final int n) throws IllegalArgumentException {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n); //NOSONAR

        if (n == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private int cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public T next() {
                if (cnt <= 0) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return e;
            }
        };
    }

    /**
     *
     *
     * @param <T>
     * @param e
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> ObjIterator<T> repeat(final T e, final long n) throws IllegalArgumentException {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n); //NOSONAR

        if (n == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public T next() {
                if (cnt <= 0) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return e;
            }
        };
    }

    /**
     * Repeats each element in the specified Collection <i>n</i> times.
     *
     * @param <T> The type of elements in the collection.
     * @param c The collection whose elements are to be repeated.
     * @param n The number of times the collection's elements are to be repeated.
     * @return An iterator over the elements in the collection, repeated<i>n</i>times.
     * @throws IllegalArgumentException if<i>n</i>is negative.
     * @see N#repeatElements(Collection, int)
     */
    public static <T> ObjIterator<T> repeatElements(final Collection<? extends T> c, final long n) throws IllegalArgumentException {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0 || N.isEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final Iterator<? extends T> iter = c.iterator();
            private T next = null;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt > 0 || iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (cnt <= 0) {
                    next = iter.next();
                    cnt = n;
                }

                cnt--;

                return next;
            }
        };
    }

    /**
     * Repeats the entire collection<i>n</i>times.
     *
     * @param <T> The type of elements in the collection.
     * @param c The collection to be repeated.
     * @param n The number of times the collection is to be repeated.
     * @return An iterator over the collection, repeated<i>n</i>times.
     * @throws IllegalArgumentException if<i>n</i>is negative.
     * @see N#repeatCollection(Collection, int)
     */
    public static <T> ObjIterator<T> repeatCollection(final Collection<? extends T> c, final long n) throws IllegalArgumentException {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0 || N.isEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private Iterator<? extends T> iter = null;
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0 || (iter != null && iter.hasNext());
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iter == null || !iter.hasNext()) {
                    iter = c.iterator();
                    cnt--;
                }

                return iter.next();
            }
        };
    }

    /**
     * Repeats each element in the specified Collection <i>n</i> times till reach the specified size.
     *
     * @param <T>
     * @param c
     * @param size
     * @return
     * @throws IllegalArgumentException
     * @see N#repeatElementsToSize(Collection, int)
     */
    public static <T> ObjIterator<T> repeatElementsToSize(final Collection<? extends T> c, final long size) throws IllegalArgumentException {
        N.checkArgument(size >= 0, "'size' can't be negative: %s", size);
        N.checkArgument(size == 0 || N.notEmpty(c), "Collection can't be empty or null when size > 0");

        if (N.isEmpty(c) || size == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final long n = size / c.size();
            private long mod = size % c.size();

            private Iterator<? extends T> iter = null;
            private T next = null;
            private long cnt = mod-- > 0 ? n + 1 : n;

            @Override
            public boolean hasNext() {
                return cnt > 0 || ((n > 0 || mod > 0) && (iter != null && iter.hasNext()));
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iter == null) {
                    iter = c.iterator();
                    next = iter.next();
                } else if (cnt <= 0) {
                    next = iter.next();
                    cnt = mod-- > 0 ? n + 1 : n;
                }

                cnt--;

                return next;
            }
        };
    }

    /**
     * Repeats the entire specified Collection till reach the specified size.
     *
     * @param <T>
     * @param c
     * @param size
     * @return
     * @throws IllegalArgumentException
     * @see N#repeatCollectionToSize(Collection, int)
     */
    public static <T> ObjIterator<T> repeatCollectionToSize(final Collection<? extends T> c, final long size) throws IllegalArgumentException {
        N.checkArgument(size >= 0, "'size' can't be negative: %s", size);
        N.checkArgument(size == 0 || N.notEmpty(c), "Collection can't be empty or null when size > 0");

        if (N.isEmpty(c) || size == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private Iterator<? extends T> iter = null;
            private long cnt = size;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iter == null || !iter.hasNext()) {
                    iter = c.iterator();
                }

                cnt--;

                return iter.next();
            }
        };
    }

    /**
     * Returns an infinite iterator cycling over the provided elements.
     * However if the provided elements are empty, an empty iterator will be returned.
     *
     * @param <T> The type of elements in the array.
     * @param elements The array whose elements are to be cycled over.
     * @return An iterator cycling over the elements of the array.
     */
    @SafeVarargs
    public static <T> ObjIterator<T> cycle(final T... elements) {
        if (N.isEmpty(elements)) {
            return ObjIterator.empty();
        }

        final T[] a = elements.clone();
        final int len = a.length;

        return new ObjIterator<>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() throws NoSuchElementException { // NOSONAR
                if (cursor >= len) {
                    cursor = 0;
                }

                return a[cursor++];
            }
        };
    }

    /**
     * Returns an infinite iterator cycling over the elements of the provided iterable.
     * However if the provided elements are empty, an empty iterator will be returned.
     *
     * @param <T> The type of elements in the iterable.
     * @param iterable The iterable whose elements are to be cycled over.
     * @return An iterator cycling over the elements of the iterable.
     */
    public static <T> ObjIterator<T> cycle(final Iterable<? extends T> iterable) {
        if (N.isEmpty(iterable)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private Iterator<? extends T> iter;
            private List<T> list;
            private T[] a;
            private int len;
            private int cursor = 0;
            private T next = null;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                if (iter == null) {
                    iter = iterable.iterator();
                    list = new ArrayList<>();
                }

                if (a == null) {
                    if (iter.hasNext()) {
                        next = iter.next();
                        list.add(next);
                        return next;
                    } else {
                        a = (T[]) list.toArray();
                        len = a.length;
                        list = null;
                    }
                }

                if (cursor >= len) {
                    cursor = 0;
                }

                return a[cursor++];
            }
        };
    }

    /**
     * Returns an iterator that cycles over the elements of the provided iterable for a specified number of rounds.
     * If the provided iterable is empty, an empty iterator will be returned.
     * If the number of rounds is zero, an empty iterator will be returned.
     *
     * @param <T> The type of elements in the iterable.
     * @param iterable The iterable whose elements are to be cycled over.
     * @param rounds The number of times to cycle over the iterable's elements.
     * @return An iterator cycling over the elements of the iterable for the specified number of rounds.
     * @throws IllegalArgumentException if 'rounds' is negative.
     */
    public static <T> ObjIterator<T> cycle(final Iterable<? extends T> iterable, final long rounds) {
        N.checkArgNotNegative(rounds, cs.rounds);

        if (N.isEmpty(iterable) || rounds == 0) {
            return ObjIterator.empty();
        } else if (rounds == 1) {
            return ObjIterator.of(iterable);
        }

        return new ObjIterator<>() {
            private Iterator<? extends T> iter;
            private List<T> list;
            private T[] a;
            private int len;
            private long m = 1;
            private int cursor = 0;
            private T next = null;

            @Override
            public boolean hasNext() {
                return m < rounds || (m == rounds && cursor < len);
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iter == null) {
                    iter = iterable.iterator();
                    list = new ArrayList<>();
                }

                if (a == null) {
                    if (iter.hasNext()) {
                        next = iter.next();
                        list.add(next);
                        return next;
                    } else {
                        m++;
                        a = (T[]) list.toArray();
                        len = a.length;
                        list = null;
                    }
                }

                if (cursor >= len) {
                    m++;
                    cursor = 0;
                }

                return a[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple boolean arrays into a single BooleanIterator.
     *
     * @param a The boolean arrays to be concatenated.
     * @return A BooleanIterator that will iterate over the elements of each provided boolean array in order.
     */
    @SafeVarargs
    public static BooleanIterator concat(final boolean[]... a) {
        if (N.isEmpty(a)) {
            return BooleanIterator.EMPTY;
        }

        return new BooleanIterator() {
            private final Iterator<boolean[]> iter = Arrays.asList(a).iterator();
            private boolean[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public boolean nextBoolean() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple char arrays into a single CharIterator.
     *
     * @param a The char arrays to be concatenated.
     * @return A CharIterator that will iterate over the elements of each provided char array in order.
     */
    @SafeVarargs
    public static CharIterator concat(final char[]... a) {
        if (N.isEmpty(a)) {
            return CharIterator.EMPTY;
        }

        return new CharIterator() {
            private final Iterator<char[]> iter = Arrays.asList(a).iterator();
            private char[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public char nextChar() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple byte arrays into a single ByteIterator.
     *
     * @param a The byte arrays to be concatenated.
     * @return A ByteIterator that will iterate over the elements of each provided byte array in order.
     */
    @SafeVarargs
    public static ByteIterator concat(final byte[]... a) {
        if (N.isEmpty(a)) {
            return ByteIterator.EMPTY;
        }

        return new ByteIterator() {
            private final Iterator<byte[]> iter = Arrays.asList(a).iterator();
            private byte[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public byte nextByte() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple short arrays into a single ShortIterator.
     *
     * @param a The short arrays to be concatenated.
     * @return A ShortIterator that will iterate over the elements of each provided short array in order.
     */
    @SafeVarargs
    public static ShortIterator concat(final short[]... a) {
        if (N.isEmpty(a)) {
            return ShortIterator.EMPTY;
        }

        return new ShortIterator() {
            private final Iterator<short[]> iter = Arrays.asList(a).iterator();
            private short[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public short nextShort() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple int arrays into a single IntIterator.
     *
     * @param a The int arrays to be concatenated.
     * @return An IntIterator that will iterate over the elements of each provided int array in order.
     */
    @SafeVarargs
    public static IntIterator concat(final int[]... a) {
        if (N.isEmpty(a)) {
            return IntIterator.EMPTY;
        }

        return new IntIterator() {
            private final Iterator<int[]> iter = Arrays.asList(a).iterator();
            private int[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public int nextInt() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple long arrays into a single LongIterator.
     *
     * @param a The long arrays to be concatenated.
     * @return A LongIterator that will iterate over the elements of each provided long array in order.
     */
    @SafeVarargs
    public static LongIterator concat(final long[]... a) {
        if (N.isEmpty(a)) {
            return LongIterator.EMPTY;
        }

        return new LongIterator() {
            private final Iterator<long[]> iter = Arrays.asList(a).iterator();
            private long[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public long nextLong() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple float arrays into a single FloatIterator.
     *
     * @param a The float arrays to be concatenated.
     * @return A FloatIterator that will iterate over the elements of each provided float array in order.
     */
    @SafeVarargs
    public static FloatIterator concat(final float[]... a) {
        if (N.isEmpty(a)) {
            return FloatIterator.EMPTY;
        }

        return new FloatIterator() {
            private final Iterator<float[]> iter = Arrays.asList(a).iterator();
            private float[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public float nextFloat() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple double arrays into a single DoubleIterator.
     *
     * @param a The double arrays to be concatenated.
     * @return A DoubleIterator that will iterate over the elements of each provided double array in order.
     */
    @SafeVarargs
    public static DoubleIterator concat(final double[]... a) {
        if (N.isEmpty(a)) {
            return DoubleIterator.EMPTY;
        }

        return new DoubleIterator() {
            private final Iterator<double[]> iter = Arrays.asList(a).iterator();
            private double[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public double nextDouble() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        };
    }

    /**
     * Concatenates multiple BooleanIterators into a single BooleanIterator.
     *
     * @param a The BooleanIterators to be concatenated.
     * @return A BooleanIterator that will iterate over the elements of each provided BooleanIterator in order.
     */
    @SafeVarargs
    public static BooleanIterator concat(final BooleanIterator... a) {
        if (N.isEmpty(a)) {
            return BooleanIterator.EMPTY;
        }

        return new BooleanIterator() {
            private final Iterator<BooleanIterator> iter = Arrays.asList(a).iterator();
            private BooleanIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public boolean nextBoolean() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextBoolean();
            }
        };
    }

    /**
     * Concatenates multiple CharIterators into a single CharIterator.
     *
     * @param a The CharIterators to be concatenated.
     * @return A CharIterator that will iterate over the elements of each provided CharIterator in order.
     */
    @SafeVarargs
    public static CharIterator concat(final CharIterator... a) {
        if (N.isEmpty(a)) {
            return CharIterator.EMPTY;
        }

        return new CharIterator() {
            private final Iterator<CharIterator> iter = Arrays.asList(a).iterator();
            private CharIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public char nextChar() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextChar();
            }
        };
    }

    /**
     * Concatenates multiple ByteIterators into a single ByteIterator.
     *
     * @param a The ByteIterators to be concatenated.
     * @return A ByteIterator that will iterate over the elements of each provided ByteIterator in order.
     */
    @SafeVarargs
    public static ByteIterator concat(final ByteIterator... a) {
        if (N.isEmpty(a)) {
            return ByteIterator.EMPTY;
        }

        return new ByteIterator() {
            private final Iterator<ByteIterator> iter = Arrays.asList(a).iterator();
            private ByteIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public byte nextByte() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextByte();
            }
        };
    }

    /**
     * Concatenates multiple ShortIterators into a single ShortIterator.
     *
     * @param a The ShortIterators to be concatenated.
     * @return A ShortIterator that will iterate over the elements of each provided ShortIterator in order.
     */
    @SafeVarargs
    public static ShortIterator concat(final ShortIterator... a) {
        if (N.isEmpty(a)) {
            return ShortIterator.EMPTY;
        }

        return new ShortIterator() {
            private final Iterator<ShortIterator> iter = Arrays.asList(a).iterator();
            private ShortIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public short nextShort() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextShort();
            }
        };
    }

    /**
     * Concatenates multiple IntIterators into a single IntIterator.
     *
     * @param a The IntIterators to be concatenated.
     * @return An IntIterator that will iterate over the elements of each provided IntIterator in order.
     */
    @SafeVarargs
    public static IntIterator concat(final IntIterator... a) {
        if (N.isEmpty(a)) {
            return IntIterator.EMPTY;
        }

        return new IntIterator() {
            private final Iterator<IntIterator> iter = Arrays.asList(a).iterator();
            private IntIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public int nextInt() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextInt();
            }
        };
    }

    /**
     * Concatenates multiple LongIterators into a single LongIterator.
     *
     * @param a The LongIterators to be concatenated.
     * @return A LongIterator that will iterate over the elements of each provided LongIterator in order.
     */
    @SafeVarargs
    public static LongIterator concat(final LongIterator... a) {
        if (N.isEmpty(a)) {
            return LongIterator.EMPTY;
        }

        return new LongIterator() {
            private final Iterator<LongIterator> iter = Arrays.asList(a).iterator();
            private LongIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public long nextLong() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextLong();
            }
        };
    }

    /**
     * Concatenates multiple FloatIterators into a single FloatIterator.
     *
     * @param a The FloatIterators to be concatenated.
     * @return A FloatIterator that will iterate over the elements of each provided FloatIterator in order.
     */
    @SafeVarargs
    public static FloatIterator concat(final FloatIterator... a) {
        if (N.isEmpty(a)) {
            return FloatIterator.EMPTY;
        }

        return new FloatIterator() {
            private final Iterator<FloatIterator> iter = Arrays.asList(a).iterator();
            private FloatIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public float nextFloat() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextFloat();
            }
        };
    }

    /**
     * Concatenates multiple DoubleIterators into a single DoubleIterator.
     *
     * @param a The DoubleIterators to be concatenated.
     * @return A DoubleIterator that will iterate over the elements of each provided DoubleIterator in order.
     */
    @SafeVarargs
    public static DoubleIterator concat(final DoubleIterator... a) {
        if (N.isEmpty(a)) {
            return DoubleIterator.EMPTY;
        }

        return new DoubleIterator() {
            private final Iterator<DoubleIterator> iter = Arrays.asList(a).iterator();
            private DoubleIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public double nextDouble() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextDouble();
            }
        };
    }

    /**
     * Concatenates multiple arrays into a single ObjIterator.
     *
     * @param <T> The type of elements in the arrays.
     * @param a The arrays to be concatenated.
     * @return An ObjIterator that will iterate over the elements of each provided array in order.
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final T[]... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<? extends T>> list = new ArrayList<>(a.length);

        for (final T[] e : a) {
            if (N.notEmpty(e)) {
                list.add(ObjIterator.of(e));
            }
        }

        return concat(list);
    }

    /**
     * Concatenates multiple Iterators into a single ObjIterator.
     *
     * @param <T> The type of elements in the Iterators.
     * @param a The Iterators to be concatenated.
     * @return An ObjIterator that will iterate over the elements of each provided Iterator in order.
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final Iterator<? extends T>... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        return concat(Array.asList(a));
    }

    /**
     * Concatenates multiple Iterable objects into a single ObjIterator.
     *
     * @param <T> The type of elements in the Iterable objects.
     * @param a The Iterable objects to be concatenated.
     * @return An ObjIterator that will iterate over the elements of each provided Iterable.
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final Iterable<? extends T>... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<? extends T>> list = new ArrayList<>(a.length);

        for (final Iterable<? extends T> e : a) {
            list.add(N.iterate(e));
        }

        return concat(list);
    }

    /**
     * Concatenates multiple Maps into a single ObjIterator of Map.Entry.
     *
     * @param <K> The type of keys in the Maps.
     * @param <V> The type of values in the Maps.
     * @param a The Maps to be concatenated.
     * @return An ObjIterator of Map.Entry that will iterate over the entries of each provided Map.
     */
    @SafeVarargs
    public static <K, V> ObjIterator<Map.Entry<K, V>> concat(final Map<? extends K, ? extends V>... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<Map.Entry<K, V>>> list = new ArrayList<>(a.length);

        for (final Map<? extends K, ? extends V> e : a) {
            if (N.notEmpty(e)) {
                list.add(((Map<K, V>) e).entrySet().iterator());
            }
        }

        return concat(list);
    }

    /**
     * Concatenates multiple Iterators into a single ObjIterator.
     *
     * @param <T> The type of elements in the Iterators.
     * @param c The collection of Iterators to be concatenated.
     * @return An ObjIterator that will iterate over the elements of each provided Iterator in order.
     */
    public static <T> ObjIterator<T> concat(final Collection<? extends Iterator<? extends T>> c) {
        if (N.isEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
            private Iterator<? extends T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        };
    }

    /**
     * Concatenates multiple Iterable objects into a single ObjIterator.
     *
     * @param <T> The type of elements in the Iterable objects.
     * @param c The collection of Iterable objects to be concatenated.
     * @return An ObjIterator that will iterate over the elements of each provided Iterable.
     */
    public static <T> ObjIterator<T> concatIterables(final Collection<? extends Iterable<? extends T>> c) {
        if (N.isEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final Iterator<? extends Iterable<? extends T>> iter = c.iterator();
            private Iterator<? extends T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = N.iterate(iter.next());
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        };
    }

    /**
     * Concatenates multiple BiIterators into a single BiIterator.
     *
     * @param <A> The type of the first element in the BiIterator.
     * @param <B> The type of the second element in the BiIterator.
     * @param a The BiIterators to be concatenated.
     * @return A BiIterator that will iterate over the elements of each provided BiIterator in order.
     */
    @SafeVarargs
    public static <A, B> BiIterator<A, B> concat(final BiIterator<A, B>... a) {
        if (N.isEmpty(a)) {
            return BiIterator.empty();
        }

        return new BiIterator<>() {
            private final Iterator<BiIterator<A, B>> iter = Arrays.asList(a).iterator();
            private BiIterator<A, B> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.BiConsumer<? super A, ? super B, E> action) throws NoSuchElementException, E {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cur.next(action);
            }

            @Override
            public void forEachRemaining(final BiConsumer<? super A, ? super B> action) {
                final Throwables.BiConsumer<? super A, ? super B, RuntimeException> actionE = Fnn.from(action);

                while (hasNext()) {
                    cur.foreachRemaining(actionE);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                while (hasNext()) {
                    cur.foreachRemaining(action);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper);

                return new ObjIterator<>() {
                    private ObjIterator<R> mappedIter = null;

                    @Override
                    public boolean hasNext() {
                        if (mappedIter == null || !mappedIter.hasNext()) {
                            while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                                cur = iter.next();
                            }

                            if (cur != null) {
                                mappedIter = cur.map(mapper);
                            }
                        }

                        return mappedIter != null && mappedIter.hasNext();
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mappedIter.next();
                    }
                };
            }
        };
    }

    /**
     * Concatenates multiple TriIterators into a single TriIterator.
     *
     * @param <A> The type of the first element in the TriIterator.
     * @param <B> The type of the second element in the TriIterator.
     * @param <C> The type of the third element in the TriIterator.
     * @param a The TriIterators to be concatenated.
     * @return A TriIterator that will iterate over the elements of each provided TriIterator in order.
     */
    @SafeVarargs
    public static <A, B, C> TriIterator<A, B, C> concat(final TriIterator<A, B, C>... a) {
        if (N.isEmpty(a)) {
            return TriIterator.empty();
        }

        return new TriIterator<>() {
            private final Iterator<TriIterator<A, B, C>> iter = Arrays.asList(a).iterator();
            private TriIterator<A, B, C> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public Triple<A, B, C> next() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }

            @Override
            protected <E extends Exception> void next(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action)
                    throws NoSuchElementException, E {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cur.next(action);
            }

            @Override
            public void forEachRemaining(final TriConsumer<? super A, ? super B, ? super C> action) {
                while (hasNext()) {
                    cur.foreachRemaining(action);
                }
            }

            @Override
            public <E extends Exception> void foreachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                while (hasNext()) {
                    cur.foreachRemaining(action);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) throws IllegalArgumentException {
                N.checkArgNotNull(mapper);

                return new ObjIterator<>() {
                    private ObjIterator<R> mappedIter = null;

                    @Override
                    public boolean hasNext() {
                        if (mappedIter == null || !mappedIter.hasNext()) {
                            while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                                cur = iter.next();
                            }

                            if (cur != null) {
                                mappedIter = cur.map(mapper);
                            }
                        }

                        return mappedIter != null && mappedIter.hasNext();
                    }

                    @Override
                    public R next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mappedIter.next();
                    }
                };
            }
        };
    }

    /**
     * Merges two Iterators into a single ObjIterator. The order of elements in the resulting iterator is determined by the provided BiFunction 'nextSelector'.
     *
     * @param <T> The type of elements in the Iterators.
     * @param a The first Iterator to be merged.
     * @param b The second Iterator to be merged.
     * @param nextSelector A BiFunction that determines the order of elements in the resulting iterator.
     * @return An ObjIterator that will iterate over the elements of the provided Iterators in the order determined by 'nextSelector'.
     * @throws IllegalArgumentException if 'nextSelector' is {@code null}.
     */
    public static <T> ObjIterator<T> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalArgumentException {
        N.checkArgNotNull(nextSelector);

        return new ObjIterator<>() {
            private final Iterator<? extends T> iterA = a == null ? ObjIterator.<T> empty() : a;
            private final Iterator<? extends T> iterB = b == null ? ObjIterator.<T> empty() : b;
            private T nextA = null;
            private T nextB = null;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return hasNextA || hasNextB || iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public T next() {
                if (hasNextA) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
                            hasNextA = false;
                            hasNextB = true;
                            return nextA;
                        } else {
                            return nextB;
                        }
                    } else {
                        hasNextA = false;
                        return nextA;
                    }
                } else if (hasNextB) {
                    if (iterA.hasNext()) {
                        if (nextSelector.apply((nextA = iterA.next()), nextB) == MergeResult.TAKE_FIRST) {
                            return nextA;
                        } else {
                            hasNextA = true;
                            hasNextB = false;
                            return nextB;
                        }
                    } else {
                        hasNextB = false;
                        return nextB;
                    }
                } else if (iterA.hasNext()) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply((nextA = iterA.next()), (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return iterA.next();
                    }
                } else {
                    return iterB.next();
                }
            }
        };
    }

    /**
     * Merges multiple Iterators into a single ObjIterator. The order of elements in the resulting iterator is determined by the provided BiFunction 'nextSelector'.
     *
     * @param <T> The type of elements in the Iterators.
     * @param c The collection of Iterators to be merged.
     * @param nextSelector A BiFunction that determines the order of elements in the resulting iterator.
     *                     The first parameter is selected if MergeResult.TAKE_FIRST is returned, otherwise the second parameter is selected.
     * @return An ObjIterator that will iterate over the elements of the provided Iterators in the order determined by 'nextSelector'.
     * @throws IllegalArgumentException if 'nextSelector' is {@code null}.
     */
    public static <T> ObjIterator<T> merge(final Collection<? extends Iterator<? extends T>> c,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalArgumentException {
        N.checkArgNotNull(nextSelector);

        if (N.isEmpty(c)) {
            return ObjIterator.<T> empty();
        } else if (c.size() == 1) {
            return ObjIterator.<T> of(c.iterator().next());
        } else if (c.size() == 2) {
            final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
        ObjIterator<T> result = merge(iter.next(), iter.next(), nextSelector);

        while (iter.hasNext()) {
            result = merge(result, iter.next(), nextSelector);
        }

        return result;
    }

    /**
     * Merges two Iterable objects into a single ObjIterator. The order of elements in the resulting iterator is determined by the provided BiFunction 'nextSelector'.
     *
     * @param <T> The type of elements in the Iterable objects.
     * @param a The first Iterable object to be merged.
     * @param b The second Iterable object to be merged.
     * @param nextSelector A BiFunction that determines the order of elements in the resulting iterator.
     * @return An ObjIterator that will iterate over the elements of the provided Iterable objects in the order determined by 'nextSelector'.
     * @throws IllegalArgumentException if 'nextSelector' is {@code null}.
     */
    public static <T> ObjIterator<T> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) {
        final Iterator<? extends T> iterA = N.iterate(a);
        final Iterator<? extends T> iterB = N.iterate(b);

        return merge(iterA, iterB, nextSelector);

    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param collections
    //     * @param nextSelector
    //     * @return
    //     * @deprecated replaced by {@link #mergeIterables(Collection, BiFunction)}
    //     */
    //    @Deprecated
    //    public static <T> ObjIterator<T> mergeCollections(final Collection<? extends Collection<? extends T>> collections,
    //            final BiFunction<? super T, ? super T, MergeResult> nextSelector) {
    //        return mergeIterables(collections, nextSelector);
    //    }

    /**
     * Merges multiple Iterable objects into a single ObjIterator. The order of elements in the resulting iterator is determined by the provided BiFunction 'nextSelector'.
     *
     * @param <T> The type of elements in the Iterable objects.
     * @param iterables The collection of Iterable objects to be merged.
     * @param nextSelector A BiFunction that determines the order of elements in the resulting iterator.
     *                     The first parameter is selected if MergeResult.TAKE_FIRST is returned, otherwise the second parameter is selected.
     * @return An ObjIterator that will iterate over the elements of the provided Iterable objects in the order determined by 'nextSelector'.
     * @throws IllegalArgumentException if 'nextSelector' is {@code null}.
     */
    public static <T> ObjIterator<T> mergeIterables(final Collection<? extends Iterable<? extends T>> iterables,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalArgumentException {
        N.checkArgNotNull(nextSelector);

        if (N.isEmpty(iterables)) {
            return ObjIterator.<T> empty();
        } else if (iterables.size() == 1) {
            return ObjIterator.<T> of(iterables.iterator().next());
        } else if (iterables.size() == 2) {
            final Iterator<? extends Iterable<? extends T>> iter = iterables.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(iterables.size());

        for (final Iterable<? extends T> e : iterables) {
            iterList.add(N.iterate(e));
        }

        return merge(iterList, nextSelector);
    }

    /**
     * Merges two sorted Iterators into a single ObjIterator, which will iterate over the elements of each Iterator in a sorted order.
     * The elements in the Iterators should implement Comparable interface.
     *
     * @param <T> The type of elements in the Iterators, which should implement Comparable interface.
     * @param sortedA The first Iterator to be merged. It should be in non-descending order.
     * @param sortedB The second Iterator to be merged. It should be in non-descending order.
     * @return An ObjIterator that will iterate over the elements of the provided Iterators in a sorted order.
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> ObjIterator<T> mergeSorted(final Iterator<? extends T> sortedA, final Iterator<? extends T> sortedB) {
        return mergeSorted(sortedA, sortedB, N.NATURAL_COMPARATOR);
    }

    /**
     * Merges two sorted Iterators into a single ObjIterator, which will iterate over the elements of each Iterator in a sorted order.
     * The order of elements in the resulting iterator is determined by the provided Comparator 'cmp'.
     *
     * @param <T> The type of elements in the Iterators.
     * @param sortedA The first Iterator to be merged. It should be in non-descending order.
     * @param sortedB The second Iterator to be merged. It should be in non-descending order.
     * @param cmp The Comparator to determine the order of elements in the resulting iterator.
     * @return An ObjIterator that will iterate over the elements of the provided Iterators in a sorted order.
     * @throws IllegalArgumentException if 'cmp' is {@code null}.
     */
    public static <T> ObjIterator<T> mergeSorted(final Iterator<? extends T> sortedA, final Iterator<? extends T> sortedB, final Comparator<? super T> cmp)
            throws IllegalArgumentException {
        N.checkArgNotNull(cmp);

        return merge(sortedA, sortedB, MergeResult.minFirst(cmp));
    }

    /**
     * Merges two sorted Iterable objects into a single ObjIterato, which will iterate over the elements of each Iterable in a sorted order.
     * The elements in the Iterable objects should implement Comparable interface.
     *
     * @param <T> The type of elements in the Iterable objects, which should implement Comparable interface.
     * @param sortedA The first Iterable object to be merged. It should be in non-descending order.
     * @param sortedB The second Iterable object to be merged. It should be in non-descending order.
     * @return An ObjIterator that will iterate over the elements of the provided Iterable objects in a sorted order.
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> ObjIterator<T> mergeSorted(final Iterable<? extends T> sortedA, final Iterable<? extends T> sortedB) {
        return mergeSorted(sortedA, sortedB, N.NATURAL_COMPARATOR);
    }

    /**
     * Merges two sorted Iterable objects into a single ObjIterator, which will iterate over the elements of each Iterable in the order determined by the provided Comparator 'cmp'.
     *
     * @param <T> The type of elements in the Iterable objects.
     * @param sortedA The first Iterable object to be merged. It should be in non-descending order.
     * @param sortedB The second Iterable object to be merged. It should be in non-descending order.
     * @param cmp The Comparator to determine the order of elements in the resulting iterator.
     * @return An ObjIterator that will iterate over the elements of the provided Iterable objects in a sorted order.
     * @throws IllegalArgumentException if 'cmp' is {@code null}.
     */
    public static <T> ObjIterator<T> mergeSorted(final Iterable<? extends T> sortedA, final Iterable<? extends T> sortedB, final Comparator<? super T> cmp) {
        final Iterator<? extends T> iterA = N.iterate(sortedA);
        final Iterator<? extends T> iterB = N.iterate(sortedB);

        return mergeSorted(iterA, iterB, cmp);
    }

    /**
     * Zips two Iterators into a single ObjIterator, which will iterate over the elements of each Iterator in parallel.
     * The resulting elements are determined by the provided BiFunction 'zipFunction'.
     *
     * @param <A> The type of elements in the first Iterator.
     * @param <B> The type of elements in the second Iterator.
     * @param <R> The type of elements in the resulting ObjIterator.
     * @param a The first Iterator to be zipped.
     * @param b The second Iterator to be zipped.
     * @param zipFunction A BiFunction that takes an element from each Iterator and returns a new element for the resulting ObjIterator.
     * @return An ObjIterator that will iterate over the elements created by 'zipFunction'.
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterator<A> a, final Iterator<B> b, final BiFunction<? super A, ? super B, ? extends R> zipFunction) {
        return new ObjIterator<>() {
            private final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(iterA.next(), iterB.next());
            }
        };
    }

    /**
     * Zips two Iterable objects into a single ObjIterator, which will iterate over the elements of each Iterable in parallel.
     * The resulting elements are determined by the provided BiFunction 'zipFunction'.
     *
     * @param <A> The type of elements in the first Iterable.
     * @param <B> The type of elements in the second Iterable.
     * @param <R> The type of elements in the resulting ObjIterator.
     * @param a The first Iterable to be zipped.
     * @param b The second Iterable to be zipped.
     * @param zipFunction A BiFunction that takes an element from each Iterable and returns a new element for the resulting ObjIterator.
     * @return An ObjIterator that will iterate over the elements created by 'zipFunction'.
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterable<A> a, final Iterable<B> b, final BiFunction<? super A, ? super B, ? extends R> zipFunction) {
        final Iterator<A> iterA = N.iterate(a);
        final Iterator<B> iterB = N.iterate(b);

        return zip(iterA, iterB, zipFunction);
    }

    /**
     * Zips three Iterators into a single ObjIterator, which will iterate over the elements of each Iterator in parallel.
     * The resulting elements are determined by the provided TriFunction 'zipFunction'.
     *
     * @param <A> The type of elements in the first Iterator.
     * @param <B> The type of elements in the second Iterator.
     * @param <C> The type of elements in the third Iterator.
     * @param <R> The type of elements in the resulting ObjIterator.
     * @param a The first Iterator to be zipped.
     * @param b The second Iterator to be zipped.
     * @param c The third Iterator to be zipped.
     * @param zipFunction A TriFunction that takes an element from each Iterator and returns a new element for the resulting ObjIterator.
     * @return An ObjIterator that will iterate over the elements created by 'zipFunction'.
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterator<A> a, final Iterator<B> b, final Iterator<C> c,
            final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction) {
        return new ObjIterator<>() {
            private final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;
            private final Iterator<C> iterC = c == null ? ObjIterator.<C> empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(iterA.next(), iterB.next(), iterC.next());
            }
        };
    }

    /**
     * Zips three Iterable objects into a single ObjIterator, which will iterate over the elements of each Iterable in parallel.
     * The resulting elements are determined by the provided TriFunction 'zipFunction'.
     *
     * @param <A> The type of elements in the first Iterable.
     * @param <B> The type of elements in the second Iterable.
     * @param <C> The type of elements in the third Iterable.
     * @param <R> The type of elements in the resulting ObjIterator.
     * @param a The first Iterable to be zipped.
     * @param b The second Iterable to be zipped.
     * @param c The third Iterable to be zipped.
     * @param zipFunction A TriFunction that takes an element from each Iterable and returns a new element for the resulting ObjIterator.
     * @return An ObjIterator that will iterate over the elements created by 'zipFunction'.
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c,
            final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction) {
        final Iterator<A> iterA = N.iterate(a);
        final Iterator<B> iterB = N.iterate(b);
        final Iterator<C> iterC = N.iterate(c);

        return zip(iterA, iterB, iterC, zipFunction);
    }

    /**
     * Zips two Iterators into a single ObjIterator, using default values when one iterator is exhausted.
     * This method can be used to combine two Iterators into one, which will iterate over the elements of each Iterator in parallel.
     * When one iterator is exhausted, the provided default values are used.
     * The resulting elements are determined by the provided BiFunction 'zipFunction'.
     *
     * @param <A> The type of elements in the first Iterator.
     * @param <B> The type of elements in the second Iterator.
     * @param <R> The type of elements in the resulting ObjIterator.
     * @param a The first Iterator to be zipped.
     * @param b The second Iterator to be zipped.
     * @param valueForNoneA The default value to be used when the first Iterator is exhausted.
     * @param valueForNoneB The default value to be used when the second Iterator is exhausted.
     * @param zipFunction A BiFunction that takes an element from each Iterator and returns a new element for the resulting ObjIterator.
     * @return An ObjIterator that will iterate over the elements created by 'zipFunction'.
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterator<A> a, final Iterator<B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, ? extends R> zipFunction) {
        return new ObjIterator<>() {
            private final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public R next() {
                if (iterA.hasNext()) {
                    return zipFunction.apply(iterA.next(), iterB.hasNext() ? iterB.next() : valueForNoneB);
                } else {
                    return zipFunction.apply(valueForNoneA, iterB.next());
                }
            }
        };
    }

    /**
     * Zips two Iterable objects into a single ObjIterator, using default values when one iterator is exhausted.
     * This method can be used to combine two Iterable objects into one, which will iterate over the elements of each Iterable in parallel.
     * When one iterator is exhausted, the provided default values are used.
     * The resulting elements are determined by the provided BiFunction 'zipFunction'.
     *
     * @param <A> The type of elements in the first Iterable.
     * @param <B> The type of elements in the second Iterable.
     * @param <R> The type of elements in the resulting ObjIterator.
     * @param a The first Iterable to be zipped.
     * @param b The second Iterable to be zipped.
     * @param valueForNoneA The default value to be used when the first Iterable is exhausted.
     * @param valueForNoneB The default value to be used when the second Iterable is exhausted.
     * @param zipFunction A BiFunction that takes an element from each Iterable and returns a new element for the resulting ObjIterator.
     * @return An ObjIterator that will iterate over the elements created by 'zipFunction'.
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterable<A> a, final Iterable<B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, ? extends R> zipFunction) {
        final Iterator<A> iterA = N.iterate(a);
        final Iterator<B> iterB = N.iterate(b);

        return zip(iterA, iterB, valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zips three Iterators into a single ObjIterator, using default values when one iterator is exhausted.
     * This method can be used to combine three Iterators into one, which will iterate over the elements of each Iterator in parallel.
     * When one iterator is exhausted, the provided default values are used.
     * The resulting elements are determined by the provided TriFunction 'zipFunction'.
     *
     * @param <A> The type of elements in the first Iterator.
     * @param <B> The type of elements in the second Iterator.
     * @param <C> The type of elements in the third Iterator.
     * @param <R> The type of elements in the resulting ObjIterator.
     * @param a The first Iterator to be zipped.
     * @param b The second Iterator to be zipped.
     * @param c The third Iterator to be zipped.
     * @param valueForNoneA The default value to be used when the first Iterator is exhausted.
     * @param valueForNoneB The default value to be used when the second Iterator is exhausted.
     * @param zipFunction A TriFunction that takes an element from each Iterator and returns a new element for the resulting ObjIterator.
     * @return An ObjIterator that will iterate over the elements created by 'zipFunction'.
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterator<A> a, final Iterator<B> b, final Iterator<C> c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction) {
        return new ObjIterator<>() {
            private final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;
            private final Iterator<C> iterC = c == null ? ObjIterator.<C> empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            @Override
            public R next() {
                if (iterA.hasNext()) {
                    return zipFunction.apply(iterA.next(), iterB.hasNext() ? iterB.next() : valueForNoneB, iterC.hasNext() ? iterC.next() : valueForNoneC);
                } else if (iterB.hasNext()) {
                    return zipFunction.apply(valueForNoneA, iterB.next(), iterC.hasNext() ? iterC.next() : valueForNoneC);
                } else {
                    return zipFunction.apply(valueForNoneA, valueForNoneB, iterC.next());
                }
            }
        };
    }

    /**
     * Zips three Iterable objects into a single ObjIterator, using default values when one iterator is exhausted.
     * This method can be used to combine three Iterable objects into one, which will iterate over the elements of each Iterable in parallel.
     * When one iterator is exhausted, the provided default values are used.
     * The resulting elements are determined by the provided TriFunction 'zipFunction'.
     *
     * @param <A> The type of elements in the first Iterable.
     * @param <B> The type of elements in the second Iterable.
     * @param <C> The type of elements in the third Iterable.
     * @param <R> The type of elements in the resulting ObjIterator.
     * @param a The first Iterable to be zipped.
     * @param b The second Iterable to be zipped.
     * @param c The third Iterable to be zipped.
     * @param valueForNoneA The default value to be used when the first Iterable is exhausted.
     * @param valueForNoneB The default value to be used when the second Iterable is exhausted.
     * @param valueForNoneC The default value to be used when the third Iterable is exhausted.
     * @param zipFunction A TriFunction that takes an element from each Iterable and returns a new element for the resulting ObjIterator.
     * @return An ObjIterator that will iterate over the elements created by 'zipFunction'.
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction) {
        final Iterator<A> iterA = N.iterate(a);
        final Iterator<B> iterB = N.iterate(b);
        final Iterator<C> iterC = N.iterate(c);

        return zip(iterA, iterB, iterC, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Unzips an Iterator into a BiIterator.
     * The transformation is determined by the provided BiConsumer 'unzip'.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param <A> The type of the first element in the resulting BiIterator.
     * @param <B> The type of the second element in the resulting BiIterator.
     * @param iter The original Iterator to be unzipped.
     * @param unzip A BiConsumer that takes an element from the original Iterator and a Pair to be filled with the resulting elements for the BiIterator.
     * @return A BiIterator that will iterate over the elements created by 'unzip'.
     * @see BiIterator#unzip(Iterator, BiConsumer)
     * @see TriIterator#unzip(Iterator, BiConsumer)
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Pair<A, B>> unzip) {
        return BiIterator.unzip(iter, unzip);
    }

    /**
     * Unzips an Iterable into a BiIterator.
     * The transformation is determined by the provided BiConsumer 'unzip'.
     *
     * @param <T> The type of elements in the original Iterable.
     * @param <A> The type of the first element in the resulting BiIterator.
     * @param <B> The type of the second element in the resulting BiIterator.
     * @param c The original Iterable to be unzipped.
     * @param unzip A BiConsumer that takes an element from the original Iterable and a Pair to be filled with the resulting elements for the BiIterator.
     * @return A BiIterator that will iterate over the elements created by 'unzip'.
     * @see BiIterator#unzip(Iterator, BiConsumer)
     * @see TriIterator#unzip(Iterator, BiConsumer)
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterable<? extends T> c, final BiConsumer<? super T, Pair<A, B>> unzip) {
        return BiIterator.unzip(N.iterate(c), unzip);
    }

    /**
     * Unzips an Iterator into a TriIterator.
     * The transformation is determined by the provided BiConsumer 'unzip'.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param <A> The type of the first element in the resulting TriIterator.
     * @param <B> The type of the second element in the resulting TriIterator.
     * @param <C> The type of the third element in the resulting TriIterator.
     * @param iter The original Iterator to be unzipped.
     * @param unzip A BiConsumer that takes an element from the original Iterator and a Triple to be filled with the resulting elements for the TriIterator.
     * @return A TriIterator that will iterate over the elements created by 'unzip'.
     * @deprecated replaced by {@link TriIterator#unzip(Iterator, BiConsumer)}
     * @see TriIterator#unzip(Iterator, BiConsumer)
     * @see TriIterator#toMultiList(Supplier)
     * @see TriIterator#toMultiSet(Supplier)
     */
    @Deprecated
    @Beta
    public static <T, A, B, C> TriIterator<A, B, C> unzipp(final Iterator<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzip) {
        return TriIterator.unzip(iter, unzip);
    }

    /**
     * Unzips an Iterable into a TriIterator.
     * The transformation is determined by the provided BiConsumer 'unzip'.
     *
     * @param <T> The type of elements in the original Iterable.
     * @param <A> The type of the first element in the resulting TriIterator.
     * @param <B> The type of the second element in the resulting TriIterator.
     * @param <C> The type of the third element in the resulting TriIterator.
     * @param c The original Iterable to be unzipped.
     * @param unzip A BiConsumer that takes an element from the original Iterable and a Triple to be filled with the resulting elements for the TriIterator.
     * @return A TriIterator that will iterate over the elements created by 'unzip'.
     * @deprecated replaced by {@link TriIterator#unzip(Iterable, BiConsumer)}
     * @see TriIterator#unzip(Iterable, BiConsumer)
     * @see TriIterator#toMultiList(Supplier)
     * @see TriIterator#toMultiSet(Supplier)
     */
    @Deprecated
    @Beta
    public static <T, A, B, C> TriIterator<A, B, C> unzipp(final Iterable<? extends T> c, final BiConsumer<? super T, Triple<A, B, C>> unzip) {
        return TriIterator.unzip(N.iterate(c), unzip);
    }

    /**
     * Note: copied from Google Guava under Apache license v2
     * <br />
     * Calls {@code next()} on {@code iterator}, either {@code numberToAdvance} times or until {@code hasNext()} returns {@code false}, whichever comes first.
     *
     * @param iterator The iterator to be advanced.
     * @param numberToAdvance The number of elements to advance the iterator.
     * @return The actual number of elements the iterator was advanced.
     * @throws IllegalArgumentException if <i>numberToAdvance</i> is negative.
     */
    public static long advance(final Iterator<?> iterator, final long numberToAdvance) throws IllegalArgumentException {
        N.checkArgNotNegative(numberToAdvance, cs.numberToAdvance);

        long i;

        for (i = 0; i < numberToAdvance && iterator.hasNext(); i++) {
            iterator.next();
        }

        return i;
    }

    /**
     * Skips the first<i>n</i>elements of the provided Iterator and returns a new ObjIterator starting from the (n+1)th element.
     * If<i>n</i>is greater than the size of the Iterator, an empty ObjIterator will be returned.
     *
     * <br />
     * This is a lazy evaluation operation. The {@code skip} action is only triggered when {@code Iterator.hasNext()} or {@code Iterator.next()} is called.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param iter The original Iterator to be skipped.
     * @param n The number of elements to skip from the beginning of the Iterator.
     * @return A new ObjIterator that will iterate over the elements of the original Iterator starting from the (n+1)th element.
     * @throws IllegalArgumentException if<i>n</i>is negative.
     */
    public static <T> ObjIterator<T> skip(final Iterator<? extends T> iter, final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (iter == null) {
            return ObjIterator.empty();
        } else if (n <= 0) {
            return ObjIterator.of(iter);
        }

        return new ObjIterator<>() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.next();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.next();
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns a new ObjIterator that is limited to the specified count of elements from the original Iterator.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param iter The original Iterator to be limited.
     * @param count The maximum number of elements to be iterated over from the original Iterator.
     * @return A new ObjIterator that will iterate over up to 'count' elements of the original Iterator.
     * @throws IllegalArgumentException if 'count' is negative.
     */
    public static <T> ObjIterator<T> limit(final Iterator<? extends T> iter, final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (iter == null || count == 0) {
            return ObjIterator.empty();
        } else if (count == Long.MAX_VALUE) {
            return ObjIterator.of(iter);
        }

        return new ObjIterator<>() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.next();
            }
        };
    }

    /**
     * Calls {@code next()} on {@code iterator}, either {@code offset} times or until {@code hasNext()} returns {@code false}, whichever comes first.
     *
     * This is a lazy evaluation operation. The {@code skip} action is only triggered when {@code Iterator.hasNext()} or {@code Iterator.next()} is called.
     *
     *
     * @param <T>
     * @param iter
     * @param offset
     * @param count
     * @return
     * @see N#slice(Iterator, int, int)
     */
    public static <T> ObjIterator<T> skipAndLimit(final Iterator<? extends T> iter, final long offset, final long count) {
        checkOffsetCount(offset, count);

        if (iter == null) {
            return ObjIterator.empty();
        }

        if (offset == 0 && count == Long.MAX_VALUE) {
            return ObjIterator.of(iter);
        } else if (offset == 0) {
            return limit(iter, count);
        } else if (count == Long.MAX_VALUE) {
            return skip(iter, offset);
        }

        return new ObjIterator<>() {
            private long cnt = count;
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return cnt > 0 && iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.next();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < offset && iter.hasNext()) {
                    iter.next();
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns a new ObjIterator that starts from the specified offset and is limited to the specified count of elements from the original Iterable.
     *
     * @param <T> The type of elements in the original Iterable.
     * @param iter The original Iterable to be skipped and limited.
     * @param offset The number of elements to skip from the beginning of the Iterable.
     * @param count The maximum number of elements to be iterated over from the Iterable after skipping.
     * @return A new ObjIterator that will iterate over up to 'count' elements of the original Iterable starting from the (offset+1)th element.
     * @throws IllegalArgumentException if 'offset' or 'count' is negative.
     */
    public static <T> ObjIterator<T> skipAndLimit(final Iterable<? extends T> iter, final long offset, final long count) {
        checkOffsetCount(offset, count);

        return iter == null ? ObjIterator.<T> empty() : skipAndLimit(iter.iterator(), offset, count);
    }

    //    /**
    //     * Returns a new {@code ObjIterator} with {@code null} elements removed.
    //     *
    //     * @param <T>
    //     * @param iter
    //     * @return
    //     * @deprecated Use {@link #skipNulls(Iterator)} instead
    //     */
    //    @Deprecated
    //    public static <T> ObjIterator<T> skipNull(final Iterator<? extends T> iter) {
    //        return skipNulls(iter);
    //    }

    /**
     * Returns a new {@code ObjIterator} with {@code null} elements removed.
     *
     * @param <T>
     * @param c
     * @return
     */
    @Beta
    public static <T> ObjIterator<T> skipNulls(final Iterable<? extends T> c) {
        return filter(c, Fn.<T> notNull());
    }

    /**
     * Returns a new {@code ObjIterator} with {@code null} elements removed.
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> ObjIterator<T> skipNulls(final Iterator<? extends T> iter) {
        return filter(iter, Fn.<T> notNull());
    }

    /**
     * Returns a new ObjIterator with distinct elements from the original Iterable.
     *
     * @param <T> The type of elements in the original Iterable.
     * @param c The original Iterable to be processed for distinct elements.
     * @return A new ObjIterator that will iterate over the distinct elements of the original Iterable.
     */
    @Beta
    public static <T> ObjIterator<T> distinct(final Iterable<? extends T> c) {
        if (c == null) {
            return ObjIterator.empty();
        }

        return distinct(c.iterator());
    }

    /**
     * Returns a new ObjIterator with distinct elements from the original Iterator.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param iter The original Iterator to be processed for distinct elements.
     * @return A new ObjIterator that will iterate over the distinct elements of the original Iterator.
     */
    public static <T> ObjIterator<T> distinct(final Iterator<? extends T> iter) {
        if (iter == null) {
            return ObjIterator.empty();
        }

        final Set<T> set = new HashSet<>();

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private T tmp = null;

            @Override
            public boolean hasNext() {
                if (next == NONE) {
                    while (iter.hasNext()) {
                        tmp = iter.next();

                        if (set.add(tmp)) {
                            next = tmp;
                            break;
                        }
                    }
                }

                return next != NONE;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    /**
     * Returns a new ObjIterator with distinct elements from the original Iterable based on a key derived from each element.
     * The key for each element is determined by the provided Function 'keyMapper'.
     *
     * @param <T> The type of elements in the original Iterable.
     * @param c The original Iterable to be processed for distinct elements.
     * @param keyMapper A Function that takes an element from the Iterable and returns a key. Elements with the same key are considered duplicates.
     * @return A new ObjIterator that will iterate over the distinct elements of the original Iterable based on the keys derived from 'keyMapper'.
     */
    @Beta
    public static <T> ObjIterator<T> distinctBy(final Iterable<? extends T> c, final Function<? super T, ?> keyMapper) throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper, cs.keyMapper);

        if (c == null) {
            return ObjIterator.empty();
        }

        return distinctBy(c.iterator(), keyMapper);
    }

    /**
     * Returns a new ObjIterator with distinct elements from the original Iterator based on a key derived from each element.
     * The key for each element is determined by the provided Function 'keyMapper'.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param iter The original Iterator to be processed for distinct elements.
     * @param keyMapper A Function that takes an element from the Iterator and returns a key. Elements with the same key are considered duplicates.
     * @return A new ObjIterator that will iterate over the distinct elements of the original Iterator based on the keys derived from 'keyMapper'.
     */
    public static <T> ObjIterator<T> distinctBy(final Iterator<? extends T> iter, final Function<? super T, ?> keyMapper) throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper, cs.keyMapper);

        if (iter == null) {
            return ObjIterator.empty();
        }

        final Set<Object> set = new HashSet<>();

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private T tmp = null;

            @Override
            public boolean hasNext() {
                if (next == NONE) {
                    while (iter.hasNext()) {
                        tmp = iter.next();

                        if (set.add(keyMapper.apply(tmp))) {
                            next = tmp;
                            break;
                        }
                    }
                }

                return next != NONE;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    /**
     * Returns a new ObjIterator that only includes elements from the original Iterable that satisfy the provided Predicate.
     *
     * @param <T> The type of elements in the original Iterable.
     * @param c The original Iterable to be filtered.
     * @param predicate A Predicate that tests each element from the Iterable. Only elements that return 'true' are included in the resulting ObjIterator.
     * @return A new ObjIterator that will iterate over the elements of the original Iterable that satisfy the provided Predicate.
     */
    @Beta
    public static <T> ObjIterator<T> filter(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        if (c == null) {
            return ObjIterator.empty();
        }

        return filter(c.iterator(), predicate);
    }

    /**
     * Returns a new ObjIterator that only includes elements from the original Iterator that satisfy the provided Predicate.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param iter The original Iterator to be filtered.
     * @param predicate A Predicate that tests each element from the Iterator. Only elements that return 'true' are included in the resulting ObjIterator.
     * @return A new ObjIterator that will iterate over the elements of the original Iterator that satisfy the provided Predicate.
     */
    public static <T> ObjIterator<T> filter(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private T tmp = null;

            @Override
            public boolean hasNext() {
                if (next == NONE) {
                    while (iter.hasNext()) {
                        tmp = iter.next();

                        if (predicate.test(tmp)) {
                            next = tmp;
                            break;
                        }
                    }
                }

                return next != NONE;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    /**
     * Returns a new ObjIterator that includes elements from the original Iterable as long as they satisfy the provided Predicate.
     * The iteration stops when an element that does not satisfy the Predicate is encountered.
     *
     * @param <T> The type of elements in the original Iterable.
     * @param c The original Iterable to be processed.
     * @param predicate A Predicate that tests each element from the Iterable. The iteration continues as long as the Predicate returns 'true'.
     * @return A new ObjIterator that will iterate over the elements of the original Iterable as long as they satisfy the provided Predicate.
     */
    @Beta
    public static <T> ObjIterator<T> takeWhile(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        if (c == null) {
            return ObjIterator.empty();
        }

        return takeWhile(c.iterator(), predicate);
    }

    /**
     * Returns a new ObjIterator that includes elements from the original Iterator as long as they satisfy the provided Predicate.
     * The iteration stops when an element that does not satisfy the Predicate is encountered.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param iter The original Iterator to be processed.
     * @param predicate A Predicate that tests each element from the Iterator. The iteration continues as long as the Predicate returns 'true'.
     * @return A new ObjIterator that will iterate over the elements of the original Iterator as long as they satisfy the provided Predicate.
     */
    public static <T> ObjIterator<T> takeWhile(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private T tmp = null;
            private boolean hasMore = true;

            @Override
            public boolean hasNext() {
                if (next == NONE && hasMore && iter.hasNext()) {
                    tmp = iter.next();

                    if (predicate.test(tmp)) {
                        next = tmp;
                    } else {
                        hasMore = false;
                    }
                }

                return next != NONE;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    /**
     * Returns a new ObjIterator that includes elements from the original Iterable as long as they satisfy the provided Predicate.
     * The iteration stops after the first element that does not satisfy the Predicate is encountered, but includes that element.
     *
     * @param <T> The type of elements in the original Iterable.
     * @param c The original Iterable to be processed.
     * @param predicate A Predicate that tests each element from the Iterable. The iteration continues as long as the Predicate returns 'true', including the first element that returns 'false'.
     * @return A new ObjIterator that will iterate over the elements of the original Iterable as long as they satisfy the provided Predicate, including the first element that does not satisfy the Predicate.
     */
    @Beta
    public static <T> ObjIterator<T> takeWhileInclusive(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        if (c == null) {
            return ObjIterator.empty();
        }

        return takeWhileInclusive(c.iterator(), predicate);
    }

    /**
     * Returns a new ObjIterator that includes elements from the original Iterator as long as they satisfy the provided Predicate.
     * The iteration stops after the first element that does not satisfy the Predicate is encountered, but includes that element.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param iter The original Iterator to be processed.
     * @param predicate A Predicate that tests each element from the Iterator. The iteration continues as long as the Predicate returns 'true', including the first element that returns 'false'.
     * @return A new ObjIterator that will iterate over the elements of the original Iterator as long as they satisfy the provided Predicate, including the first element that does not satisfy the Predicate.
     */
    public static <T> ObjIterator<T> takeWhileInclusive(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private T tmp = null;
            private boolean hasMore = true;

            @Override
            public boolean hasNext() {
                if (next == NONE && hasMore && iter.hasNext()) {
                    tmp = iter.next();

                    if (predicate.test(tmp)) {
                        next = tmp;
                    } else {
                        next = tmp;
                        hasMore = false;
                    }
                }

                return next != NONE;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    /**
     * Returns a new ObjIterator that skips elements from the original Iterable as long as they satisfy the provided Predicate.
     * The iteration begins when an element that does not satisfy the Predicate is encountered.
     *
     * @param <T> The type of elements in the original Iterable.
     * @param c The original Iterable to be processed.
     * @param predicate A Predicate that tests each element from the Iterable. The iteration skips elements as long as the Predicate returns 'true'.
     * @return A new ObjIterator that will iterate over the elements of the original Iterable starting from the first element that does not satisfy the provided Predicate.
     */
    @Beta
    public static <T> ObjIterator<T> dropWhile(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        if (c == null) {
            return ObjIterator.empty();
        }

        return dropWhile(c.iterator(), predicate);
    }

    /**
     * Returns a new ObjIterator that skips elements from the original Iterator as long as they satisfy the provided Predicate.
     * The iteration begins when an element that does not satisfy the Predicate is encountered.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param iter The original Iterator to be processed.
     * @param predicate A Predicate that tests each element from the Iterator. The iteration skips elements as long as the Predicate returns 'true'.
     * @return A new ObjIterator that will iterate over the elements of the original Iterator starting from the first element that does not satisfy the provided Predicate.
     */
    public static <T> ObjIterator<T> dropWhile(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private boolean hasDropped = false;

            @Override
            public boolean hasNext() {
                if (!hasDropped) {
                    while (iter.hasNext()) {
                        next = iter.next();

                        if (predicate.test(next)) {
                            next = NONE;
                        } else {
                            hasDropped = true;
                            break;
                        }
                    }
                }

                return next != NONE || iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (next != NONE) {
                    final T tmp = next;
                    next = NONE;
                    return tmp;
                } else {
                    return iter.next();
                }
            }
        };
    }

    /**
     * Skips elements in the provided Iterable until the provided Predicate returns {@code true}.
     * This method can be used to ignore elements in an Iterable until a certain condition is met.
     *
     * @param <T> The type of elements in the original Iterable.
     * @param c The original Iterable to be processed.
     * @param predicate A Predicate that tests elements from the original Iterable.
     * @return An ObjIterator that will iterate over the remaining elements after the Predicate returns {@code true} for the first time.
     */
    @Beta
    public static <T> ObjIterator<T> skipUntil(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        if (c == null) {
            return ObjIterator.empty();
        }

        return skipUntil(c.iterator(), predicate);
    }

    /**
     * Skips elements in the provided Iterator until the provided Predicate returns {@code true}.
     * This method can be used to ignore elements in an Iterator until a certain condition is met.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param iter The original Iterator to be processed.
     * @param predicate A Predicate that tests elements from the original Iterator.
     * @return An ObjIterator that will iterate over the remaining elements after the Predicate returns {@code true} for the first time.
     */
    public static <T> ObjIterator<T> skipUntil(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final T NONE = (T) N.NULL_MASK; //NOSONAR
            private T next = NONE;
            private boolean hasSkipped = false;

            @Override
            public boolean hasNext() {
                if (!hasSkipped) {
                    while (iter.hasNext()) {
                        next = iter.next();

                        if (predicate.test(next)) {
                            hasSkipped = true;
                            break;
                        } else {
                            next = NONE;
                        }
                    }
                }

                return next != NONE || iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (next != NONE) {
                    final T tmp = next;
                    next = NONE;
                    return tmp;
                } else {
                    return iter.next();
                }
            }
        };
    }

    /**
     * Transforms the elements of the given Iterable using the provided Function and returns a new ObjIterator with the transformed elements.
     *
     * @param <T> The type of elements in the original Iterable.
     * @param <U> The type of elements in the resulting ObjIterator.
     * @param c The original Iterable to be transformed.
     * @param mapper A Function that takes an element from the Iterable and returns a transformed element for the resulting ObjIterator.
     * @return A new ObjIterator that will iterate over the transformed elements of the original Iterable.
     */
    @Beta
    public static <T, U> ObjIterator<U> map(final Iterable<? extends T> c, final Function<? super T, U> mapper) {
        if (c == null) {
            return ObjIterator.empty();
        }

        return map(c.iterator(), mapper);
    }

    /**
     * Transforms the elements of the given Iterator using the provided Function and returns a new ObjIterator with the transformed elements.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param <U> The type of elements in the resulting ObjIterator.
     * @param iter The original Iterator to be transformed.
     * @param mapper A Function that takes an element from the Iterator and returns a transformed element for the resulting ObjIterator.
     * @return A new ObjIterator that will iterate over the transformed elements of the original Iterator.
     */
    public static <T, U> ObjIterator<U> map(final Iterator<? extends T> iter, final Function<? super T, U> mapper) {
        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public U next() {
                return mapper.apply(iter.next());
            }
        };
    }

    /**
     * Transforms the elements of the given Iterable into Iterables using the provided Function and flattens the result into a new ObjIterator.
     *
     * @param <T> The type of elements in the original Iterable.
     * @param <U> The type of elements in the resulting ObjIterator.
     * @param c The original Iterable to be transformed.
     * @param mapper A Function that takes an element from the Iterable and returns an Iterable of transformed elements.
     * @return A new ObjIterator that will iterate over the transformed elements of the original Iterable.
     */
    @Beta
    public static <T, U> ObjIterator<U> flatMap(final Iterable<? extends T> c, final Function<? super T, ? extends Iterable<? extends U>> mapper) {
        if (c == null) {
            return ObjIterator.empty();
        }

        return flatMap(c.iterator(), mapper);
    }

    /**
     * Transforms the elements of the given Iterator into Iterables using the provided Function and flattens the result into a new ObjIterator.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param <U> The type of elements in the resulting ObjIterator.
     * @param iter The original Iterator to be transformed.
     * @param mapper A Function that takes an element from the Iterator and returns an Iterable of transformed elements.
     * @return A new ObjIterator that will iterate over the transformed elements of the original Iterator.
     */
    public static <T, U> ObjIterator<U> flatMap(final Iterator<? extends T> iter, final Function<? super T, ? extends Iterable<? extends U>> mapper) {
        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private Iterable<? extends U> c = null;
            private Iterator<? extends U> cur = null;

            @Override
            public boolean hasNext() {
                if (cur == null || !cur.hasNext()) {
                    while (iter.hasNext()) {
                        c = mapper.apply(iter.next());
                        cur = c == null ? null : c.iterator();

                        if (cur != null && cur.hasNext()) {
                            break;
                        }
                    }
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public U next() throws IllegalArgumentException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        };
    }

    /**
     * Transforms the elements of the given Iterable into arrays using the provided Function and flattens the result into a new ObjIterator.
     *
     * @param <T> The type of elements in the original Iterable.
     * @param <U> The type of elements in the resulting ObjIterator.
     * @param c The original Iterable to be transformed.
     * @param mapper A Function that takes an element from the Iterable and returns an array of transformed elements.
     * @return A new ObjIterator that will iterate over the transformed elements of the original Iterable.
     */
    @Beta
    public static <T, U> ObjIterator<U> flatmap(final Iterable<? extends T> c, final Function<? super T, ? extends U[]> mapper) { //NOSONAR
        if (c == null) {
            return ObjIterator.empty();
        }

        return flatmap(c.iterator(), mapper);
    }

    /**
     * Transforms the elements of the given Iterator into arrays using the provided Function and flattens the result into a new ObjIterator.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param <U> The type of elements in the resulting ObjIterator.
     * @param iter The original Iterator to be transformed.
     * @param mapper A Function that takes an element from the Iterator and returns an array of transformed elements.
     * @return A new ObjIterator that will iterate over the transformed elements of the original Iterator.
     */
    public static <T, U> ObjIterator<U> flatmap(final Iterator<? extends T> iter, final Function<? super T, ? extends U[]> mapper) { //NOSONAR
        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private U[] a = null;
            private int len = 0;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                if (cursor >= len) {
                    while (iter.hasNext()) {
                        a = mapper.apply(iter.next());
                        len = N.len(a);
                        cursor = 0;

                        if (len > 0) {
                            break;
                        }
                    }
                }

                return cursor < len;
            }

            @Override
            public U next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }
        };
    }

    /**
     * Performs an action for each element of the given Iterator
     *
     * @param <T> The type of elements in the original Iterator.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param iter The original Iterator to be processed.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterator.
     * @throws E if the elementConsumer encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iter, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given Iterator and executes a final action upon completion.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param <E2> The type of exception that can be thrown by the onComplete action.
     * @param iter The original Iterator to be processed.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterator.
     * @param onComplete A Runnable action to be executed after all elements in the Iterator have been processed.
     * @throws E if the elementConsumer encounters an exception.
     * @throws E2 if the onComplete action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iter, 0, Long.MAX_VALUE, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given Iterator, starting from a specified offset and up to a specified count.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param iter The original Iterator to be processed.
     * @param offset The starting point in the Iterator from where elements will be processed.
     * @param count The maximum number of elements to be processed from the Iterator.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterator.
     * @throws E if the elementConsumer encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iter, offset, count, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given Iterator, starting from a specified offset and up to a specified count.
     * After all elements have been processed, a final action is executed.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param <E2> The type of exception that can be thrown by the onComplete action.
     * @param iter The original Iterator to be processed.
     * @param offset The starting point in the Iterator from where elements will be processed.
     * @param count The maximum number of elements to be processed from the Iterator.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterator.
     * @param onComplete A Runnable action to be executed after all elements in the Iterator have been processed.
     * @throws E if the elementConsumer encounters an exception.
     * @throws E2 if the onComplete action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iter, offset, count, 0, 0, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given Iterator, starting from a specified offset and up to a specified count.
     * This method also supports multi-threading with a specified number of threads and queue size.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param iter The original Iterator to be processed.
     * @param offset The starting point in the Iterator from where elements will be processed.
     * @param count The maximum number of elements to be processed from the Iterator.
     * @param processThreadNum The number of threads to be used for processing.
     * @param queueSize The size of the queue for holding elements before processing.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterator.
     * @throws E if the elementConsumer encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iter, offset, count, processThreadNum, queueSize, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given Iterator, starting from a specified offset and up to a specified count.
     * This method also supports multi-threading with a specified number of threads and queue size.
     *
     * @param <T> The type of elements in the original Iterator.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param <E2> The type of exception that can be thrown by the onComplete action.
     * @param iter The original Iterator to be processed.
     * @param offset The starting point in the Iterator from where processing should begin.
     * @param count The maximum number of elements to process.
     * @param processThreadNum The number of threads to be used for processing.
     * @param queueSize The size of the queue to hold the processing records. Default size is 1024.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterator.
     * @param onComplete A Runnable action to be performed once all elements have been processed.
     * @throws E if the elementConsumer encounters an exception.
     * @throws E2 if the onComplete action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete)
            throws E, E2 {
        forEach(Array.asList(iter), offset, count, 0, processThreadNum, queueSize, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given Collection of Iterators.
     *
     * @param <T> The type of elements in the original Iterators.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param iterators The original Collection of Iterators to be processed.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterators.
     * @throws E if the elementConsumer encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iterators, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given Collection of Iterators.
     * After all elements have been processed, a final action is executed.
     *
     * @param <T> The type of elements in the original Iterators.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param <E2> The type of exception that can be thrown by the onComplete action.
     * @param iterators The original Collection of Iterators to be processed.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterators.
     * @param onComplete A Runnable action to be executed after all elements in the Iterators have been processed.
     * @throws E if the elementConsumer encounters an exception.
     * @throws E2 if the onComplete action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iterators, 0, Long.MAX_VALUE, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given Collection of Iterators, starting from a specified offset and up to a specified count.
     *
     * @param <T> The type of elements in the original Iterators.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param iterators The original Collection of Iterators to be processed.
     * @param offset The starting point in the Iterators from where elements will be processed.
     * @param count The maximum number of elements to be processed from the Iterators.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterators.
     * @throws E if the elementConsumer encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iterators, offset, count, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given Collection of Iterators, starting from a specified offset and up to a specified count.
     * After all elements have been processed, a final action is executed.
     *
     * @param <T> The type of elements in the original Iterators.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param <E2> The type of exception that can be thrown by the onComplete action.
     * @param iterators The original Collection of Iterators to be processed.
     * @param offset The starting point in the Iterators from where elements will be processed.
     * @param count The maximum number of elements to be processed from the Iterators.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterators.
     * @param onComplete A Runnable action to be executed after all elements in the Iterators have been processed.
     * @throws E if the elementConsumer encounters an exception.
     * @throws E2 if the onComplete action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset,
            final long count, final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iterators, offset, count, 0, 0, 0, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given Collection of Iterators.
     * This method also supports multi-threading with a specified number of threads for reading.
     *
     * @param <T> The type of elements in the original Iterators.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param iterators The original Collection of Iterators to be processed.
     * @param readThreadNum The number of threads to be used for reading elements from the Iterators.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterators.
     * @throws E if the elementConsumer encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iterators, readThreadNum, processThreadNum, queueSize, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given Collection of Iterators.
     * This method also supports multi-threading with a specified number of threads for reading and processing, and a queue for holding elements before processing.
     * After all elements have been processed, a final action is executed.
     *
     * @param <T> The type of elements in the original Iterators.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param <E2> The type of exception that can be thrown by the onComplete action.
     * @param iterators The original Collection of Iterators to be processed.
     * @param readThreadNum The number of threads to be used for reading elements from the Iterators.
     * @param processThreadNum The number of threads to be used for processing elements.
     * @param queueSize The size of the queue for holding elements before processing.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterators.
     * @param onComplete A Runnable action to be executed after all elements in the Iterators have been processed.
     * @throws E if the elementConsumer encounters an exception.
     * @throws E2 if the onComplete action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer,
            final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iterators, 0, Long.MAX_VALUE, readThreadNum, processThreadNum, queueSize, elementConsumer, onComplete);
    }

    /**
     * Performs an action for each element of the given Collection of Iterators, starting from a specified offset and up to a specified count.
     * This method also supports multi-threading with a specified number of threads for reading and processing, and a queue for holding elements before processing.
     *
     * @param <T> The type of elements in the original Iterators.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param iterators The original Collection of Iterators to be processed.
     * @param offset The starting point in the Iterators from where elements will be processed.
     * @param count The maximum number of elements to be processed from the Iterators.
     * @param readThreadNum The number of threads to be used for reading elements from the Iterators.
     * @param processThreadNum The number of threads to be used for processing elements.
     * @param queueSize The size of the queue for holding elements before processing.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterators.
     * @throws E if the elementConsumer encounters an exception.
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset, final long count,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iterators, offset, count, readThreadNum, processThreadNum, queueSize, elementConsumer, Fn.emptyAction());
    }

    /**
     * Performs an action for each element of the given Collection of Iterators, starting from a specified offset and up to a specified count.
     * This method also supports multi-threading with a specified number of threads for reading and processing, and a queue size for holding the processing records.
     *
     * @param <T> The type of elements in the original Iterators.
     * @param <E> The type of exception that can be thrown by the elementConsumer.
     * @param <E2> The type of exception that can be thrown by the onComplete action.
     * @param iterators The original Collection of Iterators to be processed.
     * @param offset The starting point in the Iterators from where processing should begin.
     * @param count The maximum number of elements to process.
     * @param readThreadNum The number of threads to be used for reading.
     * @param processThreadNum The number of threads to be used for processing.
     * @param queueSize The size of the queue to hold the processing records.
     * @param elementConsumer A Consumer that performs an action on each element in the Iterators.
     * @param onComplete A Runnable action to be performed once all elements have been processed.
     * @throws IllegalArgumentException if 'offset' or 'count' is negative.
     * @throws E if the elementConsumer encounters an exception.
     * @throws E2 if the onComplete action encounters an exception.
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset,
            final long count, final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer,
            final Throwables.Runnable<E2> onComplete) throws IllegalArgumentException, E, E2 {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can not be negative", offset, count);

        if (N.isEmpty(iterators)) {
            return;
        }

        final long startTime = System.currentTimeMillis();

        if (logger.isInfoEnabled()) {
            logger.info("### Start to process: sizeOfIterators=" + iterators.size() + ", offset=" + offset + ", count=" + count + ", readThreadNum="
                    + readThreadNum + ", processThreadNum=" + processThreadNum + ", queueSize=" + queueSize);
        }

        final int readThreadNumToUse = readThreadNum == 0 ? 1 : readThreadNum;
        try (final Stream<T> stream = ((readThreadNum > 0 || queueSize > 0)
                ? Stream.parallelConcatIterators(iterators, readThreadNumToUse, (queueSize == 0 ? calculateBufferedSize(readThreadNumToUse) : queueSize))
                : Stream.concatIterators(iterators))) {

            final Iterator<? extends T> iteratorII = stream.skip(offset).limit(count).iterator();

            if (processThreadNum == 0) {
                while (iteratorII.hasNext()) {
                    elementConsumer.accept(iteratorII.next());
                }

                if (onComplete != null) {
                    onComplete.run();
                }
            } else {
                final CountDownLatch countDownLatch = new CountDownLatch(processThreadNum);
                final ExecutorService executorService = Executors.newFixedThreadPool(processThreadNum);
                final Holder<Exception> errorHolder = new Holder<>();

                for (int i = 0; i < processThreadNum; i++) {
                    executorService.execute(() -> {
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

                                elementConsumer.accept(element);
                            }
                        } catch (final Exception e) {
                            synchronized (errorHolder) {
                                if (errorHolder.value() == null) {
                                    errorHolder.setValue(e);
                                } else {
                                    errorHolder.value().addSuppressed(e);
                                }
                            }
                        } finally {
                            countDownLatch.countDown();
                        }
                    });
                }

                try {
                    countDownLatch.await();
                } catch (final InterruptedException e) {
                    N.toRuntimeException(e);
                }

                if (errorHolder.value() == null && onComplete != null) {
                    try {
                        onComplete.run();
                    } catch (final Exception e) {
                        errorHolder.setValue(e);
                    }
                }

                if (errorHolder.value() != null) {
                    throw ExceptionUtil.toRuntimeException(errorHolder.value());
                }
            }
        } finally {
            if (logger.isInfoEnabled()) {
                logger.info("### End to process. Elapsed time: " + (System.currentTimeMillis() - startTime) + " ms");
            }
        }
    }

    /**
     *
     * @param offset
     * @param count
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative.
     */
    static void checkOffsetCount(final int offset, final int count) throws IllegalArgumentException {
        if (offset < 0 || count < 0) {
            throw new IllegalArgumentException("offset: " + offset + " and count: " + count + " can't be negative");
        }
    }

    /**
     *
     * @param offset
     * @param count
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative.
     */
    static void checkOffsetCount(final long offset, final long count) throws IllegalArgumentException {
        if (offset < 0 || count < 0) {
            throw new IllegalArgumentException("offset: " + offset + " and count: " + count + " can't be negative");
        }
    }

    static int calculateBufferedSize(final int readThreadNum) {
        return N.min(1024, readThreadNum * 64);
    }
}

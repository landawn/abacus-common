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
 * This is a utility class mostly for {@code Iterator}.
 * </p>
 *
 * <p>
 * The methods in this class should only read the input {@code Collection/Array/Iterator} parameters, not modify them.
 * </p>
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Maps
 * @see com.landawn.abacus.util.Strings
 */
public final class Iterators {

    private static final Logger logger = LoggerFactory.getLogger(Iterators.class);

    private Iterators() {
        // Utility class.
    }

    /**
     *
     * @param <T>
     * @param iter
     * @param index
     * @return
     */
    public static <T> Nullable<T> get(final Iterator<? extends T> iter, long index) {
        N.checkArgNotNegative(index, "index");

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
     *
     * @param iter
     * @param valueToFind
     * @return
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
     *
     * @param iter
     * @return
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
     *
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param predicate
     * @return
     * @throws E
     */
    public static <T, E extends Exception> long count(final Iterator<? extends T> iter, final Throwables.Predicate<? super T, E> predicate) throws E {
        N.checkArgNotNull(predicate, "predicate"); //NOSONAR

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
     *
     * @param <T>
     * @param e
     * @param n
     * @return
     */
    public static <T> ObjIterator<T> repeat(final T e, final int n) {
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
     * @param <T>
     * @param e
     * @param n
     * @return
     */
    public static <T> ObjIterator<T> repeat(final T e, final long n) {
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
     * Repeats the elements in the specified Collection one by one.
     *
     * @param <T>
     * @param c
     * @param n
     * @return
     * @see N#repeatElements(Collection, int)
     */
    public static <T> ObjIterator<T> repeatElements(final Collection<? extends T> c, final long n) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0 || N.isEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private Iterator<? extends T> iter = c.iterator();
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
     * Repeats the whole specified Collection {@code n} times.
     *
     * @param <T>
     * @param c
     * @param n
     * @return
     * @see N#repeatCollection(Collection, int)
     */
    public static <T> ObjIterator<T> repeatCollection(final Collection<? extends T> c, final long n) {
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
     * Repeats the elements in the specified Collection one by one till reach the specified size.
     *
     * @param <T>
     * @param c
     * @param size
     * @return
     * @see N#repeatElementsToSize(Collection, int)
     */
    public static <T> ObjIterator<T> repeatElementsToSize(final Collection<? extends T> c, final long size) {
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
     * Repeats the whole specified Collection till reach the specified size.
     *
     * @param <T>
     * @param c
     * @param size
     * @return
     * @see N#repeatCollectionToSize(Collection, int)
     */
    public static <T> ObjIterator<T> repeatCollectionToSize(final Collection<? extends T> c, final long size) {
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final T[]... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<? extends T>> list = new ArrayList<>(a.length);

        for (T[] e : a) {
            if (N.notEmpty(e)) {
                list.add(ObjIterator.of(e));
            }
        }

        return concat(list);
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final Iterator<? extends T>... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        return concat(Array.asList(a));
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final Iterable<? extends T>... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<? extends T>> list = new ArrayList<>(a.length);

        for (Iterable<? extends T> e : a) {
            list.add(N.iterate(e));
        }

        return concat(list);
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <K, V> ObjIterator<Map.Entry<K, V>> concat(final Map<? extends K, ? extends V>... a) {
        if (N.isEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<Map.Entry<K, V>>> list = new ArrayList<>(a.length);

        for (Map<? extends K, ? extends V> e : a) {
            if (N.notEmpty(e)) {
                list.add(((Map<K, V>) e).entrySet().iterator());
            }
        }

        return concat(list);
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
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
     *
     * @param <T>
     * @param c
     * @return
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
     *
     * @param <A>
     * @param <B>
     * @param a
     * @return
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
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, ? extends R> mapper) {
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
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param a
     * @return
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
            public void forEachRemaining(TriConsumer<? super A, ? super B, ? super C> action) {
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
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, ? extends R> mapper) {
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
     *
     * @param <T>
     * @param a
     * @param b
     * @param nextSelector
     * @return
     */
    public static <T> ObjIterator<T> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) {
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
     *
     *
     * @param <T>
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> ObjIterator<T> merge(final Collection<? extends Iterator<? extends T>> c,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) {
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
     *
     * @param <T>
     * @param a
     * @param b
     * @param nextSelector
     * @return
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
     *
     *
     * @param <T>
     * @param iterables
     * @param nextSelector
     * @return
     */
    public static <T> ObjIterator<T> mergeIterables(final Collection<? extends Iterable<? extends T>> iterables,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) {
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

        for (Iterable<? extends T> e : iterables) {
            iterList.add(N.iterate(e));
        }

        return merge(iterList, nextSelector);
    }

    /**
     *
     * @param <T>
     * @param sortedA should be in non-descending order as this method does not sort its input.
     * @param sortedB should be in non-descending order as this method does not sort its input.
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> ObjIterator<T> mergeSorted(final Iterator<? extends T> sortedA, final Iterator<? extends T> sortedB) {
        return mergeSorted(sortedA, sortedB, N.NATURAL_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param sortedA should be in non-descending order as this method does not sort its input.
     * @param sortedB should be in non-descending order as this method does not sort its input.
     * @param cmp
     * @return
     */
    public static <T> ObjIterator<T> mergeSorted(final Iterator<? extends T> sortedA, final Iterator<? extends T> sortedB, final Comparator<? super T> cmp) {
        N.checkArgNotNull(cmp);

        return merge(sortedA, sortedB, MergeResult.minFirst(cmp));
    }

    /**
     *
     * @param <T>
     * @param sortedA should be in non-descending order as this method does not sort its input.
     * @param sortedB should be in non-descending order as this method does not sort its input.
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> ObjIterator<T> mergeSorted(final Iterable<? extends T> sortedA, final Iterable<? extends T> sortedB) {
        return mergeSorted(sortedA, sortedB, N.NATURAL_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param sortedA should be in non-descending order as this method does not sort its input.
     * @param sortedB should be in non-descending order as this method does not sort its input.
     * @param cmp
     * @return
     */
    public static <T> ObjIterator<T> mergeSorted(final Iterable<? extends T> sortedA, final Iterable<? extends T> sortedB, final Comparator<? super T> cmp) {
        final Iterator<? extends T> iterA = N.iterate(sortedA);
        final Iterator<? extends T> iterB = N.iterate(sortedB);

        return mergeSorted(iterA, iterB, cmp);
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param a
     * @param b
     * @param zipFunction
     * @return
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterator<A> a, final Iterator<B> b, final BiFunction<? super A, ? super B, ? extends R> zipFunction) {
        N.checkArgNotNull(zipFunction);

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
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param a
     * @param b
     * @param zipFunction
     * @return
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterable<A> a, final Iterable<B> b, final BiFunction<? super A, ? super B, ? extends R> zipFunction) {
        final Iterator<A> iterA = N.iterate(a);
        final Iterator<B> iterB = N.iterate(b);

        return zip(iterA, iterB, zipFunction);
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @return
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterator<A> a, final Iterator<B> b, final Iterator<C> c,
            final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction) {
        N.checkArgNotNull(zipFunction);

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
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @return
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c,
            final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction) {
        final Iterator<A> iterA = N.iterate(a);
        final Iterator<B> iterB = N.iterate(b);
        final Iterator<C> iterC = N.iterate(c);

        return zip(iterA, iterB, iterC, zipFunction);
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @return
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterator<A> a, final Iterator<B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, ? extends R> zipFunction) {
        N.checkArgNotNull(zipFunction);

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
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @return
     */
    public static <A, B, R> ObjIterator<R> zip(final Iterable<A> a, final Iterable<B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, ? extends R> zipFunction) {
        final Iterator<A> iterA = N.iterate(a);
        final Iterator<B> iterB = N.iterate(b);

        return zip(iterA, iterB, valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @return
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
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @return
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, ? extends R> zipFunction) {
        final Iterator<A> iterA = N.iterate(a);
        final Iterator<B> iterB = N.iterate(b);
        final Iterator<C> iterC = N.iterate(c);

        return zip(iterA, iterB, iterC, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <B>
     * @param iter
     * @param unzip the second parameter is an output parameter.
     * @return
     * @see BiIterator#unzip(Iterator, BiConsumer)
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Pair<A, B>> unzip) {
        return BiIterator.unzip(iter, unzip);
    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <B>
     * @param c
     * @param unzip the second parameter is an output parameter.
     * @return
     * @see BiIterator#unzip(Iterable, BiConsumer)
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Iterable<? extends T> c, final BiConsumer<? super T, Pair<A, B>> unzip) {
        return BiIterator.unzip(N.iterate(c), unzip);
    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <B>
     * @param <C>
     * @param iter
     * @param unzip the second parameter is an output parameter.
     * @return
     * @see TriIterator#unzip(Iterator, BiConsumer)
     */
    @Beta
    public static <T, A, B, C> TriIterator<A, B, C> unzipp(final Iterator<? extends T> iter, final BiConsumer<? super T, Triple<A, B, C>> unzip) {
        return TriIterator.unzip(iter, unzip);
    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <B>
     * @param <C>
     * @param c
     * @param unzip the second parameter is an output parameter.
     * @return
     * @see TriIterator#unzip(Iterable, BiConsumer)
     */
    @Beta
    public static <T, A, B, C> TriIterator<A, B, C> unzipp(final Iterable<? extends T> c, final BiConsumer<? super T, Triple<A, B, C>> unzip) {
        return TriIterator.unzip(N.iterate(c), unzip);
    }

    /**
     * Note: copied from Google Guava under Apache license v2
     * <br />
     * Calls {@code next()} on {@code iterator}, either {@code numberToAdvance} times
     * or until {@code hasNext()} returns {@code false}, whichever comes first.
     *
     * @param iterator
     * @param numberToAdvance
     * @return
     */
    public static long advance(Iterator<?> iterator, long numberToAdvance) {
        N.checkArgNotNegative(numberToAdvance, "numberToAdvance");

        long i;

        for (i = 0; i < numberToAdvance && iterator.hasNext(); i++) {
            iterator.next();
        }

        return i;
    }

    /**
     * Calls {@code next()} on {@code iterator}, either {@code n} times
     * or until {@code hasNext()} returns {@code false}, whichever comes first.
     *
     * This is a lazy evaluation operation. The {@code skip} action is only triggered when {@code Iterator.hasNext()} or {@code Iterator.next()} is called.
     *
     * @param <T>
     * @param iter
     * @param n
     * @return
     */
    public static <T> ObjIterator<T> skip(final Iterator<? extends T> iter, final long n) {
        N.checkArgNotNegative(n, "n");

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
     * Returns a new {@code Iterator}.
     *
     * @param <T>
     * @param iter
     * @param count
     * @return
     */
    public static <T> ObjIterator<T> limit(final Iterator<? extends T> iter, final long count) {
        N.checkArgNotNegative(count, "count");

        if (iter == null || count == 0) {
            return ObjIterator.empty();
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
     * Calls {@code next()} on {@code iterator}, either {@code offset} times
     * or until {@code hasNext()} returns {@code false}, whichever comes first.
     *
     * This is a lazy evaluation operation. The {@code skip} action is only triggered when {@code Iterator.hasNext()} or {@code Iterator.next()} is called.
     *
     *
     * @param <T>
     * @param iter
     * @param offset
     * @param count
     * @return
     */
    public static <T> ObjIterator<T> skipAndLimit(final Iterator<? extends T> iter, final long offset, final long count) {
        N.checkArgNotNegative(count, "offset");
        N.checkArgNotNegative(count, "count");

        if (iter == null) {
            return ObjIterator.empty();
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
     * Returns a new {@code ObjIterator} with {@code null} elements removed.
     *
     * @param <T>
     * @param iter
     * @return
     * @deprecated Use {@link #skipNulls(Iterator)} instead
     */
    @Deprecated
    public static <T> ObjIterator<T> skipNull(final Iterator<? extends T> iter) {
        return skipNulls(iter);
    }

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
     *
     *
     * @param <T>
     * @param c
     * @return
     */
    @Beta
    public static <T> ObjIterator<T> distinct(final Iterable<? extends T> c) {
        if (c == null) {
            return ObjIterator.empty();
        }

        return distinct(c.iterator());
    }

    /**
     *
     *
     * @param <T>
     * @param iter
     * @return
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
     *
     *
     * @param <T>
     * @param c
     * @param keyMapper
     * @return
     */
    @Beta
    public static <T> ObjIterator<T> distinctBy(final Iterable<? extends T> c, final Function<? super T, ?> keyMapper) {
        N.checkArgNotNull(keyMapper, "keyMapper");

        if (c == null) {
            return ObjIterator.empty();
        }

        return distinctBy(c.iterator(), keyMapper);
    }

    /**
     *
     *
     * @param <T>
     * @param iter
     * @param keyMapper
     * @return
     */
    public static <T> ObjIterator<T> distinctBy(final Iterator<? extends T> iter, final Function<? super T, ?> keyMapper) {
        N.checkArgNotNull(keyMapper, "keyMapper");

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
     *
     * @param <T>
     * @param c
     * @param predicate
     * @return
     */
    @Beta
    public static <T> ObjIterator<T> filter(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, "predicate");

        if (c == null) {
            return ObjIterator.empty();
        }

        return filter(c.iterator(), predicate);
    }

    /**
     *
     * @param <T>
     * @param iter
     * @param predicate
     * @return
     */
    public static <T> ObjIterator<T> filter(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, "predicate");

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
     *
     * @param <T>
     * @param c
     * @param predicate
     * @return
     */
    @Beta
    public static <T> ObjIterator<T> takeWhile(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, "predicate");

        if (c == null) {
            return ObjIterator.empty();
        }

        return takeWhile(c.iterator(), predicate);
    }

    /**
     *
     *
     * @param <T>
     * @param iter
     * @param predicate
     * @return
     */
    public static <T> ObjIterator<T> takeWhile(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, "predicate");

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
     *
     * @param <T>
     * @param c
     * @param predicate
     * @return
     */
    @Beta
    public static <T> ObjIterator<T> takeWhileInclusive(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, "predicate");

        if (c == null) {
            return ObjIterator.empty();
        }

        return takeWhileInclusive(c.iterator(), predicate);
    }

    /**
     *
     *
     * @param <T>
     * @param iter
     * @param predicate
     * @return
     */
    public static <T> ObjIterator<T> takeWhileInclusive(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, "predicate");

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
     *
     * @param <T>
     * @param c
     * @param predicate
     * @return
     */
    @Beta
    public static <T> ObjIterator<T> dropWhile(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, "predicate");

        if (c == null) {
            return ObjIterator.empty();
        }

        return dropWhile(c.iterator(), predicate);
    }

    /**
     *
     *
     * @param <T>
     * @param iter
     * @param predicate
     * @return
     */
    public static <T> ObjIterator<T> dropWhile(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, "predicate");

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
     *
     * @param <T>
     * @param c
     * @param predicate
     * @return
     */
    @Beta
    public static <T> ObjIterator<T> skipUntil(final Iterable<? extends T> c, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, "predicate");

        if (c == null) {
            return ObjIterator.empty();
        }

        return skipUntil(c.iterator(), predicate);
    }

    /**
     *
     *
     * @param <T>
     * @param iter
     * @param predicate
     * @return
     */
    public static <T> ObjIterator<T> skipUntil(final Iterator<? extends T> iter, final Predicate<? super T> predicate) {
        N.checkArgNotNull(predicate, "predicate");

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
     *
     * @param <T>
     * @param <U>
     * @param c
     * @param mapper
     * @return
     */
    @Beta
    public static <T, U> ObjIterator<U> map(final Iterable<? extends T> c, final Function<? super T, U> mapper) {
        N.checkArgNotNull(mapper, "mapper"); //NOSONAR

        if (c == null) {
            return ObjIterator.empty();
        }

        return map(c.iterator(), mapper);
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param iter
     * @param mapper
     * @return
     */
    public static <T, U> ObjIterator<U> map(final Iterator<? extends T> iter, final Function<? super T, U> mapper) {
        N.checkArgNotNull(mapper, "mapper"); //NOSONAR

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
     *
     * @param <T>
     * @param <U>
     * @param c
     * @param mapper
     * @return
     */
    @Beta
    public static <T, U> ObjIterator<U> flatMap(final Iterable<? extends T> c, final Function<? super T, ? extends Iterable<? extends U>> mapper) {
        N.checkArgNotNull(mapper, "mapper"); //NOSONAR

        if (c == null) {
            return ObjIterator.empty();
        }

        return flatMap(c.iterator(), mapper);
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param iter
     * @param mapper
     * @return
     */
    public static <T, U> ObjIterator<U> flatMap(final Iterator<? extends T> iter, final Function<? super T, ? extends Iterable<? extends U>> mapper) {
        N.checkArgNotNull(mapper, "mapper");

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
            public U next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param c
     * @param mapper
     * @return
     */
    @Beta
    public static <T, U> ObjIterator<U> flatmap(final Iterable<? extends T> c, final Function<? super T, ? extends U[]> mapper) { //NOSONAR
        N.checkArgNotNull(mapper, "mapper");

        if (c == null) {
            return ObjIterator.empty();
        }

        return flatmap(c.iterator(), mapper);
    }

    /**
     *
     *
     * @param <T>
     * @param <U>
     * @param iter
     * @param mapper
     * @return
     */
    public static <T, U> ObjIterator<U> flatmap(final Iterator<? extends T> iter, final Function<? super T, ? extends U[]> mapper) { //NOSONAR
        N.checkArgNotNull(mapper, "mapper");

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
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param elementConsumer
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iter, elementConsumer, Fn.emptyAction());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param iter
     * @param elementConsumer
     * @param onComplete
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iter, 0, Long.MAX_VALUE, elementConsumer, onComplete);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param offset
     * @param count
     * @param elementConsumer
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iter, offset, count, elementConsumer, Fn.emptyAction());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param iter
     * @param offset
     * @param count
     * @param elementConsumer
     * @param onComplete
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iter, offset, count, 0, 0, elementConsumer, onComplete);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param offset
     * @param count
     * @param processThreadNum
     * @param queueSize
     * @param elementConsumer
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, long offset, long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iter, offset, count, processThreadNum, queueSize, elementConsumer, Fn.emptyAction());
    }

    /**
     * Parse the elements in the specified iterators one by one.
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param iter
     * @param offset
     * @param count
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param elementConsumer
     * @param onComplete
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter, long offset, long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete)
            throws E, E2 {
        forEach(Array.asList(iter), offset, count, 0, processThreadNum, queueSize, elementConsumer, onComplete);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iterators
     * @param elementConsumer
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iterators, elementConsumer, Fn.emptyAction());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param iterators
     * @param elementConsumer
     * @param onComplete
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iterators, 0, Long.MAX_VALUE, elementConsumer, onComplete);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iterators
     * @param offset
     * @param count
     * @param elementConsumer
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset, final long count,
            final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iterators, offset, count, elementConsumer, Fn.emptyAction());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param iterators
     * @param offset
     * @param count
     * @param elementConsumer
     * @param onComplete
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset,
            final long count, final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iterators, offset, count, 0, 0, 0, elementConsumer, onComplete);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iterators
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param elementConsumer
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iterators, readThreadNum, processThreadNum, queueSize, elementConsumer, Fn.emptyAction());
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param iterators
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param elementConsumer
     * @param onComplete
     * @throws E the e
     * @throws E2
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer,
            final Throwables.Runnable<E2> onComplete) throws E, E2 {
        forEach(iterators, 0, Long.MAX_VALUE, readThreadNum, processThreadNum, queueSize, elementConsumer, onComplete);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iterators
     * @param offset
     * @param count
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param elementConsumer
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset, final long count,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer) throws E {
        forEach(iterators, offset, count, readThreadNum, processThreadNum, queueSize, elementConsumer, Fn.emptyAction());
    }

    /**
     * Parse the elements in the specified iterators one by one.
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param iterators
     * @param offset
     * @param count
     * @param readThreadNum new threads started to parse/process the lines/records
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param elementConsumer
     * @param onComplete
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, E extends Exception, E2 extends Exception> void forEach(final Collection<? extends Iterator<? extends T>> iterators, final long offset,
            final long count, final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super T, E> elementConsumer,
            final Throwables.Runnable<E2> onComplete) throws E, E2 {
        N.checkArgument(offset >= 0 && count >= 0, "'offset'=%s and 'count'=%s can not be negative", offset, count);

        if (N.isEmpty(iterators)) {
            return;
        }

        if (logger.isInfoEnabled()) {
            logger.info("### Start to process");
        }

        try (final Stream<T> stream = ((readThreadNum > 0 || queueSize > 0)
                ? Stream.parallelConcatIterators(iterators, (readThreadNum == 0 ? 1 : readThreadNum), (queueSize == 0 ? 1024 : queueSize))
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
                        } catch (Exception e) {
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
                } catch (InterruptedException e) {
                    N.toRuntimeException(e);
                }

                if (errorHolder.value() == null && onComplete != null) {
                    try {
                        onComplete.run();
                    } catch (Exception e) {
                        errorHolder.setValue(e);
                    }
                }

                if (errorHolder.value() != null) {
                    throw ExceptionUtil.toRuntimeException(errorHolder.value());
                }
            }
        } finally {
            if (logger.isInfoEnabled()) {
                logger.info("### End to process");
            }
        }
    }
}

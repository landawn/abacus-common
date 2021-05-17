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

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.TriFunction;

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
 * @see com.landawn.abacus.util.StringUtil
 */
public final class Iterators {

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
     * @param iter
     * @param filter
     * @return
     */
    public static <T, E extends Exception> long count(final Iterator<? extends T> iter, final Throwables.Predicate<? super T, E> filter) throws E {
        N.checkArgNotNull(filter, "filter");

        if (iter == null) {
            return 0;
        }

        long res = 0;

        while (iter.hasNext()) {
            if (filter.test(iter.next())) {
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
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private int cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public T next() {
                if (cnt <= 0) {
                    throw new NoSuchElementException();
                }

                cnt--;
                return e;
            }
        };
    }

    /**
     *
     * @param <T>
     * @param c
     * @param n
     * @return
     */
    public static <T> ObjIterator<T> repeatEach(final Collection<? extends T> c, final int n) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0 || N.isNullOrEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private Iterator<? extends T> iter = c.iterator();
            private T next = null;
            private int cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt > 0 || iter.hasNext();
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
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
     *
     * @param <T>
     * @param c
     * @param n
     * @return
     */
    public static <T> ObjIterator<T> repeatAll(final Collection<? extends T> c, final int n) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0 || N.isNullOrEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private Iterator<? extends T> iter = null;
            private int cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0 || (iter != null && iter.hasNext());
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                if (iter == null || iter.hasNext() == false) {
                    iter = c.iterator();
                    cnt--;
                }

                return iter.next();
            }
        };
    }

    /**
     * Repeat each to size.
     *
     * @param <T>
     * @param c
     * @param size
     * @return
     */
    public static <T> ObjIterator<T> repeatEachToSize(final Collection<? extends T> c, final int size) {
        N.checkArgument(size >= 0, "'size' can't be negative: %s", size);
        N.checkArgument(size == 0 || N.notNullOrEmpty(c), "Collection can't be empty or null when size > 0");

        if (N.isNullOrEmpty(c) || size == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private final int n = size / c.size();
            private int mod = size % c.size();

            private Iterator<? extends T> iter = null;
            private T next = null;
            private int cnt = mod-- > 0 ? n + 1 : n;

            @Override
            public boolean hasNext() {
                return cnt > 0 || ((n > 0 || mod > 0) && (iter != null && iter.hasNext()));
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
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
     * Repeat all to size.
     *
     * @param <T>
     * @param c
     * @param size
     * @return
     */
    public static <T> ObjIterator<T> repeatAllToSize(final Collection<? extends T> c, final int size) {
        N.checkArgument(size >= 0, "'size' can't be negative: %s", size);
        N.checkArgument(size == 0 || N.notNullOrEmpty(c), "Collection can't be empty or null when size > 0");

        if (N.isNullOrEmpty(c) || size == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private Iterator<? extends T> iter = null;
            private int cnt = size;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                if (iter == null || iter.hasNext() == false) {
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
        if (N.isNullOrEmpty(a)) {
            return BooleanIterator.EMPTY;
        }

        return new BooleanIterator() {
            private final Iterator<boolean[]> iter = Arrays.asList(a).iterator();
            private boolean[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isNullOrEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public boolean nextBoolean() {
                if ((cur == null || cursor >= cur.length) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return ShortIterator.EMPTY;
        }

        return new ShortIterator() {
            private final Iterator<short[]> iter = Arrays.asList(a).iterator();
            private short[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isNullOrEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public short nextShort() {
                if ((cur == null || cursor >= cur.length) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return ByteIterator.EMPTY;
        }

        return new ByteIterator() {
            private final Iterator<byte[]> iter = Arrays.asList(a).iterator();
            private byte[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isNullOrEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public byte nextByte() {
                if ((cur == null || cursor >= cur.length) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return IntIterator.EMPTY;
        }

        return new IntIterator() {
            private final Iterator<int[]> iter = Arrays.asList(a).iterator();
            private int[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isNullOrEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public int nextInt() {
                if ((cur == null || cursor >= cur.length) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return LongIterator.EMPTY;
        }

        return new LongIterator() {
            private final Iterator<long[]> iter = Arrays.asList(a).iterator();
            private long[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isNullOrEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public long nextLong() {
                if ((cur == null || cursor >= cur.length) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return FloatIterator.EMPTY;
        }

        return new FloatIterator() {
            private final Iterator<float[]> iter = Arrays.asList(a).iterator();
            private float[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isNullOrEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public float nextFloat() {
                if ((cur == null || cursor >= cur.length) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return DoubleIterator.EMPTY;
        }

        return new DoubleIterator() {
            private final Iterator<double[]> iter = Arrays.asList(a).iterator();
            private double[] cur;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                while ((N.isNullOrEmpty(cur) || cursor >= cur.length) && iter.hasNext()) {
                    cur = iter.next();
                    cursor = 0;
                }

                return cur != null && cursor < cur.length;
            }

            @Override
            public double nextDouble() {
                if ((cur == null || cursor >= cur.length) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return BooleanIterator.EMPTY;
        }

        return new BooleanIterator() {
            private final Iterator<BooleanIterator> iter = Arrays.asList(a).iterator();
            private BooleanIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public boolean nextBoolean() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return CharIterator.EMPTY;
        }

        return new CharIterator() {
            private final Iterator<CharIterator> iter = Arrays.asList(a).iterator();
            private CharIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public char nextChar() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return ByteIterator.EMPTY;
        }

        return new ByteIterator() {
            private final Iterator<ByteIterator> iter = Arrays.asList(a).iterator();
            private ByteIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public byte nextByte() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return ShortIterator.EMPTY;
        }

        return new ShortIterator() {
            private final Iterator<ShortIterator> iter = Arrays.asList(a).iterator();
            private ShortIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public short nextShort() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return IntIterator.EMPTY;
        }

        return new IntIterator() {
            private final Iterator<IntIterator> iter = Arrays.asList(a).iterator();
            private IntIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public int nextInt() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return LongIterator.EMPTY;
        }

        return new LongIterator() {
            private final Iterator<LongIterator> iter = Arrays.asList(a).iterator();
            private LongIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public long nextLong() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return FloatIterator.EMPTY;
        }

        return new FloatIterator() {
            private final Iterator<FloatIterator> iter = Arrays.asList(a).iterator();
            private FloatIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public float nextFloat() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return DoubleIterator.EMPTY;
        }

        return new DoubleIterator() {
            private final Iterator<DoubleIterator> iter = Arrays.asList(a).iterator();
            private DoubleIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public double nextDouble() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<? extends T>> list = new ArrayList<>(a.length);

        for (T[] e : a) {
            if (N.notNullOrEmpty(e)) {
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
    public static <T> ObjIterator<T> concat(final Collection<? extends T>... a) {
        if (N.isNullOrEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<? extends T>> list = new ArrayList<>(a.length);

        for (Collection<? extends T> e : a) {
            if (N.notNullOrEmpty(e)) {
                list.add(e.iterator());
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
    public static <K, V> ObjIterator<Map.Entry<K, V>> concat(final Map<? extends K, ? extends V>... a) {
        if (N.isNullOrEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<Map.Entry<K, V>>> list = new ArrayList<>(a.length);

        for (Map<? extends K, ? extends V> e : a) {
            if (N.notNullOrEmpty(e)) {
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
    public static <T> ObjIterator<T> concatt(final Collection<? extends Collection<? extends T>> c) {
        if (N.isNullOrEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private final Iterator<? extends Collection<? extends T>> iter = c.iterator();
            private Iterator<? extends T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    final Collection<? extends T> c = iter.next();
                    cur = N.isNullOrEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
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
    public static <T> ObjIterator<T> concat(final Iterator<? extends T>... a) {
        if (N.isNullOrEmpty(a)) {
            return ObjIterator.empty();
        }

        return concat(Array.asList(a));
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> ObjIterator<T> concat(final Collection<? extends Iterator<? extends T>> c) {
        if (N.isNullOrEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
            private Iterator<? extends T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return BiIterator.empty();
        }

        return new BiIterator<A, B>() {
            private final Iterator<BiIterator<A, B>> iter = Arrays.asList(a).iterator();
            private BiIterator<A, B> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }

            @Override
            public <E extends Exception> void forEachRemaining(final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
                while (hasNext()) {
                    cur.forEachRemaining(action);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final BiFunction<? super A, ? super B, R> mapper) {
                N.checkArgNotNull(mapper);

                return new ObjIterator<R>() {
                    private ObjIterator<R> mappedIter = null;

                    @Override
                    public boolean hasNext() {
                        if (mappedIter == null || mappedIter.hasNext() == false) {
                            while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
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
                        if (hasNext() == false) {
                            throw new NoSuchElementException();
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
        if (N.isNullOrEmpty(a)) {
            return TriIterator.empty();
        }

        return new TriIterator<A, B, C>() {
            private final Iterator<TriIterator<A, B, C>> iter = Arrays.asList(a).iterator();
            private TriIterator<A, B, C> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public Triple<A, B, C> next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }

            @Override
            public <E extends Exception> void forEachRemaining(final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
                while (hasNext()) {
                    cur.forEachRemaining(action);
                }
            }

            @Override
            public <R> ObjIterator<R> map(final TriFunction<? super A, ? super B, ? super C, R> mapper) {
                N.checkArgNotNull(mapper);

                return new ObjIterator<R>() {
                    private ObjIterator<R> mappedIter = null;

                    @Override
                    public boolean hasNext() {
                        if (mappedIter == null || mappedIter.hasNext() == false) {
                            while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
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
                        if (hasNext() == false) {
                            throw new NoSuchElementException();
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
    public static <T> ObjIterator<T> merge(final Collection<? extends T> a, final Collection<? extends T> b,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) {
        final Iterator<? extends T> iterA = N.isNullOrEmpty(a) ? ObjIterator.<T> empty() : (Iterator<? extends T>) a.iterator();
        final Iterator<? extends T> iterB = N.isNullOrEmpty(b) ? ObjIterator.<T> empty() : (Iterator<? extends T>) b.iterator();

        return merge(iterA, iterB, nextSelector);

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

        return new ObjIterator<T>() {
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
     * @param <T>
     * @param collections
     * @param nextSelector
     * @return
     * @deprecated replaced by {@link #mergeCollections(Collection, BiFunction)}
     */
    @Deprecated
    public static <T> ObjIterator<T> merge(final List<? extends Collection<? extends T>> collections,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) {
        return mergeCollections(collections, nextSelector);
    }

    /**
     * 
     * @param <T>
     * @param collections
     * @param nextSelector
     * @return
     * @deprecated replaced by {@link #mergeCollections(Collection, BiFunction)}
     */
    @Deprecated
    public static <T> ObjIterator<T> mergge(final Collection<? extends Collection<? extends T>> collections,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) {
        return mergeCollections(collections, nextSelector);
    }

    public static <T> ObjIterator<T> mergeCollections(final Collection<? extends Collection<? extends T>> collections,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector);

        if (N.isNullOrEmpty(collections)) {
            return ObjIterator.<T> empty();
        } else if (collections.size() == 1) {
            return ObjIterator.<T> of(collections.iterator().next());
        } else if (collections.size() == 2) {
            final Iterator<? extends Collection<? extends T>> iter = collections.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(collections.size());

        for (Collection<? extends T> e : collections) {
            iterList.add(N.iterate(e));
        }

        return merge(iterList, nextSelector);
    }

    /**
     *
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> ObjIterator<T> merge(final Collection<? extends Iterator<? extends T>> c,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector);

        if (N.isNullOrEmpty(c)) {
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
     * @param a should be in non-descending order as this method does not sort its input.
     * @param b should be in non-descending order as this method does not sort its input. 
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> ObjIterator<T> mergeSorted(final Collection<? extends T> a, final Collection<? extends T> b) {
        return mergeSorted(a, b, Comparators.<T> naturalOrder());
    }

    /**
     *
     * @param <T>
     * @param a should be in non-descending order as this method does not sort its input.
     * @param b should be in non-descending order as this method does not sort its input.
     * @param cmp
     * @return
     */
    public static <T> ObjIterator<T> mergeSorted(final Collection<? extends T> a, final Collection<? extends T> b, final Comparator<? super T> cmp) {
        final Iterator<? extends T> iterA = N.isNullOrEmpty(a) ? ObjIterator.<T> empty() : (Iterator<? extends T>) a.iterator();
        final Iterator<? extends T> iterB = N.isNullOrEmpty(b) ? ObjIterator.<T> empty() : (Iterator<? extends T>) b.iterator();

        return mergeSorted(iterA, iterB, cmp);

    }

    /**
     *
     * @param <T>
     * @param a should be in non-descending order as this method does not sort its input.
     * @param b should be in non-descending order as this method does not sort its input. 
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> ObjIterator<T> mergeSorted(final Iterator<? extends T> a, final Iterator<? extends T> b) {
        return mergeSorted(a, b, Comparators.<T> naturalOrder());
    }

    /**
     *
     * @param <T>
     * @param a should be in non-descending order as this method does not sort its input.
     * @param b should be in non-descending order as this method does not sort its input.
     * @param cmp
     * @return
     */
    public static <T> ObjIterator<T> mergeSorted(final Iterator<? extends T> a, final Iterator<? extends T> b, final Comparator<? super T> cmp) {
        N.checkArgNotNull(cmp);

        return merge(a, b, MergeResult.minFirst(cmp));
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
    public static <A, B, R> ObjIterator<R> zip(final Collection<A> a, final Collection<B> b, final BiFunction<? super A, ? super B, R> zipFunction) {
        final Iterator<A> iterA = N.isNullOrEmpty(a) ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = N.isNullOrEmpty(b) ? ObjIterator.<B> empty() : b.iterator();

        return zip(iterA, iterB, zipFunction);
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
    public static <A, B, R> ObjIterator<R> zip(final Iterator<A> a, final Iterator<B> b, final BiFunction<? super A, ? super B, R> zipFunction) {
        N.checkArgNotNull(zipFunction);

        return new ObjIterator<R>() {
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
     * @param <C>
     * @param <R>
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @return
     */
    public static <A, B, C, R> ObjIterator<R> zip(final Collection<A> a, final Collection<B> b, final Collection<C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        final Iterator<A> iterA = N.isNullOrEmpty(a) ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = N.isNullOrEmpty(b) ? ObjIterator.<B> empty() : b.iterator();
        final Iterator<C> iterC = N.isNullOrEmpty(c) ? ObjIterator.<C> empty() : c.iterator();

        return zip(iterA, iterB, iterC, zipFunction);
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
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        N.checkArgNotNull(zipFunction);

        return new ObjIterator<R>() {
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
     * @param <R>
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @return
     */
    public static <A, B, R> ObjIterator<R> zip(final Collection<A> a, final Collection<B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        final Iterator<A> iterA = N.isNullOrEmpty(a) ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = N.isNullOrEmpty(b) ? ObjIterator.<B> empty() : b.iterator();

        return zip(iterA, iterB, valueForNoneA, valueForNoneB, zipFunction);
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
            final BiFunction<? super A, ? super B, R> zipFunction) {
        N.checkArgNotNull(zipFunction);

        return new ObjIterator<R>() {
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
    public static <A, B, C, R> ObjIterator<R> zip(final Collection<A> a, final Collection<B> b, final Collection<C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        final Iterator<A> iterA = N.isNullOrEmpty(a) ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = N.isNullOrEmpty(b) ? ObjIterator.<B> empty() : b.iterator();
        final Iterator<C> iterC = N.isNullOrEmpty(c) ? ObjIterator.<C> empty() : c.iterator();

        return zip(iterA, iterB, iterC, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
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
            final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return new ObjIterator<R>() {
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
     * @param <T>
     * @param <A>
     * @param <B>
     * @param iter
     * @param unzip the second parameter is an output parameter.
     * @return
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
     */
    public static <T, A, B> BiIterator<A, B> unzip(final Collection<? extends T> c, final BiConsumer<? super T, Pair<A, B>> unzip) {
        return BiIterator.unzip(c == null ? ObjIterator.<T> empty() : c.iterator(), unzip);
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
     */
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
     */
    public static <T, A, B, C> TriIterator<A, B, C> unzipp(final Collection<? extends T> c, final BiConsumer<? super T, Triple<A, B, C>> unzip) {
        return TriIterator.unzip(c == null ? ObjIterator.<T> empty() : c.iterator(), unzip);
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

        if (iter == null || n == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (skipped == false) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
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

        return new ObjIterator<T>() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
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

        return new ObjIterator<T>() {
            private long cnt = count;
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (skipped == false) {
                    skip();
                }

                return cnt > 0 && iter.hasNext();
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
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
     */
    public static <T> ObjIterator<T> skipNull(final Iterator<? extends T> iter) {
        return filter(iter, Fn.<T> notNull());
    }

    public static <T> ObjIterator<T> distinct(final Iterator<? extends T> iter) {
        if (iter == null) {
            return ObjIterator.empty();
        }

        final Set<T> set = new HashSet<>();

        return new ObjIterator<T>() {
            private final T NONE = (T) N.NULL_MASK;
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
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    public static <T> ObjIterator<T> distinctBy(final Iterator<? extends T> iter, final Function<? super T, ?> keyMapper) {
        if (iter == null) {
            return ObjIterator.empty();
        }

        final Set<Object> set = new HashSet<>();

        return new ObjIterator<T>() {
            private final T NONE = (T) N.NULL_MASK;
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
                if (hasNext() == false) {
                    throw new NoSuchElementException();
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
     * @param iter
     * @param filter
     * @return
     */
    public static <T> ObjIterator<T> filter(final Iterator<? extends T> iter, final Predicate<? super T> filter) {
        N.checkArgNotNull(filter, "filter");

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private final T NONE = (T) N.NULL_MASK;
            private T next = NONE;
            private T tmp = null;

            @Override
            public boolean hasNext() {
                if (next == NONE) {
                    while (iter.hasNext()) {
                        tmp = iter.next();

                        if (filter.test(tmp)) {
                            next = tmp;
                            break;
                        }
                    }
                }

                return next != NONE;
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    public static <T> ObjIterator<T> takeWhile(final Iterator<? extends T> iter, final Predicate<? super T> filter) {
        N.checkArgNotNull(filter, "filter");

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private final T NONE = (T) N.NULL_MASK;
            private T next = NONE;
            private T tmp = null;
            private boolean hasMore = true;

            @Override
            public boolean hasNext() {
                if (next == NONE && hasMore && iter.hasNext()) {
                    tmp = iter.next();

                    if (filter.test(tmp)) {
                        next = tmp;
                    } else {
                        hasMore = false;
                    }
                }

                return next != NONE;
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    public static <T> ObjIterator<T> takeWhileInclusive(final Iterator<? extends T> iter, final Predicate<? super T> filter) {
        N.checkArgNotNull(filter, "filter");

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private final T NONE = (T) N.NULL_MASK;
            private T next = NONE;
            private T tmp = null;
            private boolean hasMore = true;

            @Override
            public boolean hasNext() {
                if (next == NONE && hasMore && iter.hasNext()) {
                    tmp = iter.next();

                    if (filter.test(tmp)) {
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
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                tmp = next;
                next = NONE;
                return tmp;
            }
        };
    }

    public static <T> ObjIterator<T> dropWhile(final Iterator<? extends T> iter, final Predicate<? super T> filter) {
        N.checkArgNotNull(filter, "filter");

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private final T NONE = (T) N.NULL_MASK;
            private T next = NONE;
            private boolean hasDropped = false;

            @Override
            public boolean hasNext() {
                if (hasDropped == false) {
                    while (iter.hasNext()) {
                        next = iter.next();

                        if (filter.test(next)) {
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
                if (hasNext() == false) {
                    throw new NoSuchElementException();
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

    public static <T> ObjIterator<T> skipUntil(final Iterator<? extends T> iter, final Predicate<? super T> filter) {
        N.checkArgNotNull(filter, "filter");

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private final T NONE = (T) N.NULL_MASK;
            private T next = NONE;
            private boolean hasSkipped = false;

            @Override
            public boolean hasNext() {
                if (hasSkipped == false) {
                    while (iter.hasNext()) {
                        next = iter.next();

                        if (filter.test(next)) {
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
                if (hasNext() == false) {
                    throw new NoSuchElementException();
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
     * @param iter
     * @param mapper
     * @return
     */
    public static <T, U> ObjIterator<U> map(final Iterator<? extends T> iter, final Function<? super T, U> mapper) {
        N.checkArgNotNull(mapper, "mapper");

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<U>() {
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
     * @param iter
     * @param mapper
     * @return
     */
    public static <T, U> ObjIterator<U> flatMap(final Iterator<? extends T> iter, final Function<? super T, ? extends Collection<? extends U>> mapper) {
        N.checkArgNotNull(mapper, "mapper");

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<U>() {
            private Collection<? extends U> c = null;
            private Iterator<? extends U> cur = null;

            @Override
            public boolean hasNext() {
                if (cur == null || cur.hasNext() == false) {
                    while (iter.hasNext()) {
                        c = mapper.apply(iter.next());
                        cur = c == null || c.size() == 0 ? null : c.iterator();

                        if (cur != null && cur.hasNext()) {
                            break;
                        }
                    }
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public U next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }
        };
    }

    public static <T, U> ObjIterator<U> flattMap(final Iterator<? extends T> iter, final Function<? super T, ? extends U[]> mapper) {
        N.checkArgNotNull(mapper, "mapper");

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<U>() {
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
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return a[cursor++];
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param init
     * @param hasNext
     * @param supplier
     * @return
     */
    public static <T, U> ObjIterator<T> generate(final U init, final Predicate<? super U> hasNext, final Function<? super U, T> supplier) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new ObjIterator<T>() {
            @Override
            public boolean hasNext() {
                return hasNext.test(init);
            }

            @Override
            public T next() {
                return supplier.apply(init);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param init
     * @param hasNext
     * @param supplier
     * @return
     */
    public static <T, U> ObjIterator<T> generate(final U init, final BiPredicate<? super U, T> hasNext, final BiFunction<? super U, T, T> supplier) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new ObjIterator<T>() {
            private T prev = null;

            @Override
            public boolean hasNext() {
                return hasNext.test(init, prev);
            }

            @Override
            public T next() {
                return (prev = supplier.apply(init, prev));
            }
        };
    }
}

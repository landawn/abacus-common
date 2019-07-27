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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.TriFunction;

/**
 * 
 * 
 * @since 0.9
 * 
 * @author Haiyang Li
 */
public final class Iterators {
    private Iterators() {
        // singleton.
    }

    public static boolean contains(final Iterator<?> iter, final Object objToFind) {
        if (iter == null) {
            return false;
        }

        while (iter.hasNext()) {
            if (N.equals(iter.next(), objToFind)) {
                return true;
            }
        }

        return false;
    }

    public static long indexOf(final Iterator<?> iter, final Object objToFind) {
        if (iter == null) {
            return N.INDEX_NOT_FOUND;
        }

        long index = 0;

        while (iter.hasNext()) {
            if (N.equals(iter.next(), objToFind)) {
                return index;
            }

            index++;
        }

        return N.INDEX_NOT_FOUND;
    }

    public static long occurrencesOf(final Iterator<?> iter, final Object objToFind) {
        if (iter == null) {
            return 0;
        }

        long occurrences = 0;

        while (iter.hasNext()) {
            if (N.equals(iter.next(), objToFind)) {
                occurrences++;
            }
        }

        return occurrences;
    }

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

    public static <T> long count(final Iterator<T> iter, final Predicate<? super T> filter) {
        N.checkArgNotNull(filter);

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

    public static <T> List<T> toList(final Iterator<? extends T> iter) {
        if (iter == null) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>();

        while (iter.hasNext()) {
            result.add(iter.next());
        }

        return result;
    }

    public static <T> Set<T> toSet(final Iterator<? extends T> iter) {
        if (iter == null) {
            return new HashSet<>();
        }

        final Set<T> result = new HashSet<>();

        while (iter.hasNext()) {
            result.add(iter.next());
        }

        return result;
    }

    public static <T, C extends Collection<T>> C toCollection(final Iterator<? extends T> iter, final Supplier<? extends C> collectionFactory) {
        final C c = collectionFactory.get();

        if (iter == null) {
            return c;
        }

        while (iter.hasNext()) {
            c.add(iter.next());
        }

        return c;
    }

    public static <T, K, E extends Exception> Map<K, T> toMap(final Iterator<? extends T> iter, final Try.Function<? super T, K, E> keyMapper) throws E {
        N.checkArgNotNull(keyMapper);

        if (iter == null) {
            return new HashMap<>();
        }

        final Map<K, T> result = new HashMap<>();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();
            result.put(keyMapper.apply(e), e);
        }

        return result;
    }

    public static <T, K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Iterator<? extends T> iter,
            final Try.Function<? super T, K, E> keyMapper, final Try.Function<? super T, ? extends V, E2> valueExtractor) throws E, E2 {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);

        if (iter == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = new HashMap<>();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();
            result.put(keyMapper.apply(e), valueExtractor.apply(e));
        }

        return result;
    }

    public static <T, K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Iterator<? extends T> iter,
            final Try.Function<? super T, K, E> keyMapper, final Try.Function<? super T, ? extends V, E2> valueExtractor,
            final Supplier<? extends M> mapSupplier) throws E, E2 {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);

        if (iter == null) {
            return mapSupplier.get();
        }

        final M result = mapSupplier.get();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();
            result.put(keyMapper.apply(e), valueExtractor.apply(e));
        }

        return result;
    }

    public static <T, E extends Exception> void forEach(final Iterator<T> iter, final Try.Consumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        if (iter == null) {
            return;
        }

        while (iter.hasNext()) {
            action.accept(iter.next());
        }
    }

    public static <T, E extends Exception> void forEach(final Iterator<T> iter, final Try.IndexedConsumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        if (iter == null) {
            return;
        }

        int idx = 0;

        while (iter.hasNext()) {
            action.accept(idx++, iter.next());
        }
    }

    public static <T, U, E extends Exception, E2 extends Exception> void forEach(final Iterator<T> iter,
            final Try.Function<? super T, ? extends Collection<U>, E> flatMapper, final Try.BiConsumer<? super T, ? super U, E2> action) throws E, E2 {
        N.checkArgNotNull(flatMapper);
        N.checkArgNotNull(action);

        if (iter == null) {
            return;
        }

        T e = null;

        while (iter.hasNext()) {
            e = iter.next();

            final Collection<U> c2 = flatMapper.apply(e);

            if (N.notNullOrEmpty(c2)) {
                for (U u : c2) {
                    action.accept(e, u);
                }
            }
        }
    }

    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEach(final Iterator<T> iter,
            final Try.Function<? super T, ? extends Collection<T2>, E> flatMapper, final Try.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2,
            final Try.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        N.checkArgNotNull(flatMapper);
        N.checkArgNotNull(flatMapper2);
        N.checkArgNotNull(action);

        if (iter == null) {
            return;
        }

        T e = null;

        while (iter.hasNext()) {
            e = iter.next();

            final Collection<T2> c2 = flatMapper.apply(e);

            if (N.notNullOrEmpty(c2)) {
                for (T2 t2 : c2) {
                    final Collection<T3> c3 = flatMapper2.apply(t2);

                    if (N.notNullOrEmpty(c3)) {
                        for (T3 t3 : c3) {
                            action.accept(e, t2, t3);
                        }
                    }
                }
            }
        }
    }

    public static <A, B, E extends Exception> void forEach(final Iterator<A> a, final Iterator<B> b, final Try.BiConsumer<? super A, ? super B, E> action)
            throws E {
        N.checkArgNotNull(action);

        if (a == null || b == null) {
            return;
        }

        while (a.hasNext() && b.hasNext()) {
            action.accept(a.next(), b.next());
        }
    }

    public static <A, B, C, E extends Exception> void forEach(final Iterator<A> a, final Iterator<B> b, final Iterator<C> c,
            final Try.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        N.checkArgNotNull(action);

        if (a == null || b == null || c == null) {
            return;
        }

        while (a.hasNext() && b.hasNext() && c.hasNext()) {
            action.accept(a.next(), b.next(), c.next());
        }
    }

    public static <A, B, E extends Exception> void forEach(final Iterator<A> a, final Iterator<B> b, final A valueForNoneA, final B valueForNoneB,
            final Try.BiConsumer<? super A, ? super B, E> action) throws E {
        N.checkArgNotNull(action);

        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;

        A nextA = null;
        B nextB = null;

        while (iterA.hasNext() || iterB.hasNext()) {
            nextA = iterA.hasNext() ? iterA.next() : valueForNoneA;
            nextB = iterB.hasNext() ? iterB.next() : valueForNoneB;

            action.accept(nextA, nextB);
        }
    }

    public static <A, B, C, E extends Exception> void forEach(final Iterator<A> a, final Iterator<B> b, final Iterator<C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final Try.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        N.checkArgNotNull(action);

        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;
        final Iterator<C> iterC = b == null ? ObjIterator.<C> empty() : c;

        A nextA = null;
        B nextB = null;
        C nextC = null;

        while (iterA.hasNext() || iterB.hasNext() || iterC.hasNext()) {
            nextA = iterA.hasNext() ? iterA.next() : valueForNoneA;
            nextB = iterB.hasNext() ? iterB.next() : valueForNoneB;
            nextC = iterC.hasNext() ? iterC.next() : valueForNoneC;

            action.accept(nextA, nextB, nextC);
        }
    }

    public static <T, U, E extends Exception, E2 extends Exception> void forEachNonNull(final Iterator<T> iter,
            final Try.Function<? super T, ? extends Collection<U>, E> flatMapper, final Try.BiConsumer<? super T, ? super U, E2> action) throws E, E2 {
        N.checkArgNotNull(flatMapper);
        N.checkArgNotNull(action);

        if (iter == null) {
            return;
        }

        T e = null;

        while (iter.hasNext()) {
            e = iter.next();

            if (e != null) {
                final Collection<U> c2 = flatMapper.apply(e);

                if (N.notNullOrEmpty(c2)) {
                    for (U u : c2) {
                        if (u != null) {
                            action.accept(e, u);
                        }
                    }
                }
            }
        }
    }

    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEachNonNull(final Iterator<T> iter,
            final Try.Function<? super T, ? extends Collection<T2>, E> flatMapper, final Try.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2,
            final Try.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        N.checkArgNotNull(flatMapper);
        N.checkArgNotNull(flatMapper2);
        N.checkArgNotNull(action);

        if (iter == null) {
            return;
        }

        T e = null;

        while (iter.hasNext()) {
            e = iter.next();

            if (e != null) {
                final Collection<T2> c2 = flatMapper.apply(e);

                if (N.notNullOrEmpty(c2)) {
                    for (T2 t2 : c2) {
                        if (t2 != null) {
                            final Collection<T3> c3 = flatMapper2.apply(t2);

                            if (N.notNullOrEmpty(c3)) {
                                for (T3 t3 : c3) {
                                    if (t3 != null) {
                                        action.accept(e, t2, t3);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static <T, E extends Exception> void forEachPair(final Iterator<T> iter, final Try.BiConsumer<? super T, ? super T, E> action) throws E {
        forEachPair(iter, action, 1);
    }

    public static <T, E extends Exception> void forEachPair(final Iterator<T> iter, final Try.BiConsumer<? super T, ? super T, E> action, final int increment)
            throws E {
        N.checkArgNotNull(action);
        final int windowSize = 2;
        N.checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        if (iter == null) {
            return;
        }

        boolean isFirst = true;
        T prev = null;

        while (iter.hasNext()) {
            if (increment > windowSize && isFirst == false) {
                int skipNum = increment - windowSize;

                while (skipNum-- > 0 && iter.hasNext()) {
                    iter.next();
                }

                if (iter.hasNext() == false) {
                    break;
                }
            }

            if (increment == 1) {
                action.accept(isFirst ? iter.next() : prev, (prev = (iter.hasNext() ? iter.next() : null)));
            } else {
                action.accept(iter.next(), iter.hasNext() ? iter.next() : null);
            }

            isFirst = false;
        }
    }

    public static <T, E extends Exception> void forEachTriple(final Iterator<T> iter, final Try.TriConsumer<? super T, ? super T, ? super T, E> action)
            throws E {
        forEachTriple(iter, action, 1);
    }

    public static <T, E extends Exception> void forEachTriple(final Iterator<T> iter, final Try.TriConsumer<? super T, ? super T, ? super T, E> action,
            final int increment) throws E {
        N.checkArgNotNull(action);
        final int windowSize = 3;
        N.checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        if (iter == null) {
            return;
        }

        boolean isFirst = true;
        T prev = null;
        T prev2 = null;

        while (iter.hasNext()) {
            if (increment > windowSize && isFirst == false) {
                int skipNum = increment - windowSize;

                while (skipNum-- > 0 && iter.hasNext()) {
                    iter.next();
                }

                if (iter.hasNext() == false) {
                    break;
                }
            }

            if (increment == 1) {
                action.accept(isFirst ? iter.next() : prev2, (prev2 = (isFirst ? (iter.hasNext() ? iter.next() : null) : prev)),
                        (prev = (iter.hasNext() ? iter.next() : null)));
            } else if (increment == 2) {
                action.accept(isFirst ? iter.next() : prev, iter.hasNext() ? iter.next() : null, (prev = (iter.hasNext() ? iter.next() : null)));
            } else {
                action.accept(iter.next(), iter.hasNext() ? iter.next() : null, iter.hasNext() ? iter.next() : null);
            }

            isFirst = false;
        }
    }

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
    };

    public static <T> ObjIterator<T> repeatEach(final Collection<T> c, final int n) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0 || N.isNullOrEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private Iterator<T> iter = null;
            private T next = null;
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

                if (iter == null) {
                    iter = c.iterator();
                    next = iter.next();
                } else if (cnt <= 0) {
                    next = iter.next();
                    cnt = n;
                }

                cnt--;

                return next;
            }
        };
    }

    public static <T> ObjIterator<T> repeatAll(final Collection<T> c, final int n) {
        N.checkArgument(n >= 0, "'n' can't be negative: %s", n);

        if (n == 0 || N.isNullOrEmpty(c)) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private Iterator<T> iter = null;
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
    };

    public static <T> ObjIterator<T> repeatEachToSize(final Collection<T> c, final int size) {
        N.checkArgument(size >= 0, "'size' can't be negative: %s", size);
        N.checkArgument(size == 0 || N.notNullOrEmpty(c), "Collection can't be empty or null when size > 0");

        if (N.isNullOrEmpty(c) || size == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private final int n = size / c.size();
            private int mod = size % c.size();

            private Iterator<T> iter = null;
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

    public static <T> ObjIterator<T> repeatAllToSize(final Collection<T> c, final int size) {
        N.checkArgument(size >= 0, "'size' can't be negative: %s", size);
        N.checkArgument(size == 0 || N.notNullOrEmpty(c), "Collection can't be empty or null when size > 0");

        if (N.isNullOrEmpty(c) || size == 0) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private Iterator<T> iter = null;
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
    };

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

    @SafeVarargs
    public static <T> ObjIterator<T> concat(final T[]... a) {
        if (N.isNullOrEmpty(a)) {
            return ObjIterator.empty();
        }

        final List<Iterator<T>> list = new ArrayList<>(a.length);

        for (T[] e : a) {
            if (N.notNullOrEmpty(e)) {
                list.add(ObjIterator.of(e));
            }
        }

        return concat(list);
    }

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

    @SafeVarargs
    public static <T> ObjIterator<T> concat(final Iterator<? extends T>... a) {
        if (N.isNullOrEmpty(a)) {
            return ObjIterator.empty();
        }

        return concat(N.asList(a));
    }

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
            public <E extends Exception> void forEachRemaining(final Try.BiConsumer<? super A, ? super B, E> action) throws E {
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
            public <E extends Exception> void forEachRemaining(final Try.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
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

    public static <T> ObjIterator<T> merge(final Collection<? extends T> a, final Collection<? extends T> b,
            final BiFunction<? super T, ? super T, Nth> nextSelector) {
        final Iterator<T> iterA = N.isNullOrEmpty(a) ? ObjIterator.<T> empty() : (Iterator<T>) a.iterator();
        final Iterator<T> iterB = N.isNullOrEmpty(b) ? ObjIterator.<T> empty() : (Iterator<T>) b.iterator();

        return merge(iterA, iterB, nextSelector);

    }

    public static <T> ObjIterator<T> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
            final BiFunction<? super T, ? super T, Nth> nextSelector) {
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
                        if (nextSelector.apply(nextA, (nextB = iterB.next())) == Nth.FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.next()), nextB) == Nth.FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.next()), (nextB = iterB.next())) == Nth.FIRST) {
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

    public static <A, B, R> ObjIterator<R> zip(final Collection<A> a, final Collection<B> b, final BiFunction<? super A, ? super B, R> zipFunction) {
        final Iterator<A> iterA = N.isNullOrEmpty(a) ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = N.isNullOrEmpty(b) ? ObjIterator.<B> empty() : b.iterator();

        return zip(iterA, iterB, zipFunction);
    }

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

    public static <A, B, C, R> ObjIterator<R> zip(final Collection<A> a, final Collection<B> b, final Collection<C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        final Iterator<A> iterA = N.isNullOrEmpty(a) ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = N.isNullOrEmpty(b) ? ObjIterator.<B> empty() : b.iterator();
        final Iterator<C> iterC = N.isNullOrEmpty(c) ? ObjIterator.<C> empty() : c.iterator();

        return zip(iterA, iterB, iterC, zipFunction);
    }

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

    public static <A, B, R> ObjIterator<R> zip(final Collection<A> a, final Collection<B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        final Iterator<A> iterA = N.isNullOrEmpty(a) ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = N.isNullOrEmpty(b) ? ObjIterator.<B> empty() : b.iterator();

        return zip(iterA, iterB, valueForNoneA, valueForNoneB, zipFunction);
    }

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
                return zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB);
            }
        };
    }

    public static <A, B, C, R> ObjIterator<R> zip(final Collection<A> a, final Collection<B> b, final Collection<C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        final Iterator<A> iterA = N.isNullOrEmpty(a) ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = N.isNullOrEmpty(b) ? ObjIterator.<B> empty() : b.iterator();
        final Iterator<C> iterC = N.isNullOrEmpty(c) ? ObjIterator.<C> empty() : c.iterator();

        return zip(iterA, iterB, iterC, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

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
                return zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB,
                        iterC.hasNext() ? iterC.next() : valueForNoneC);
            }
        };
    }

    /**
     * 
     * @param iter
     * @param unzip the second parameter is an output parameter.
     * @return
     */
    public static <T, L, R> BiIterator<L, R> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Pair<L, R>> unzip) {
        return BiIterator.unzip(iter, unzip);
    }

    /**
     * 
     * @param iter
     * @param unzip the second parameter is an output parameter.
     * @return
     */
    public static <T, L, M, R> TriIterator<L, M, R> unzipp(final Iterator<? extends T> iter, final BiConsumer<? super T, Triple<L, M, R>> unzip) {
        return TriIterator.unzip(iter, unzip);
    }

    /**
     * 
     * @param iter
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    public static <T> ObjIterator<List<T>> split(final Iterator<? extends T> iter, final int chunkSize) {
        N.checkArgument(chunkSize > 0, "'chunkSize' must be greater than 0, can't be: %s", chunkSize);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<List<T>>() {
            private final Iterator<? extends T> iterator = iter;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public List<T> next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final List<T> next = new ArrayList<>(chunkSize);

                for (int i = 0; i < chunkSize && iterator.hasNext(); i++) {
                    next.add(iterator.next());
                }

                return next;
            }
        };
    }

    public static <T> Nullable<T> get(final Iterator<T> iter, int index) {
        N.checkArgNotNegative(index, "index");

        if (iter == null) {
            return Nullable.empty();
        }

        while (iter.hasNext()) {
            if (index-- == 0) {
                return Nullable.of(iter.next());
            } else {
                iter.next();
            }
        }

        return Nullable.empty();
    }

    public static <T> Nullable<T> first(final Iterator<T> iter) {
        return iter != null && iter.hasNext() ? Nullable.of(iter.next()) : Nullable.<T> empty();
    }

    public static <T> Optional<T> firstNonNull(final Iterator<T> iter) {
        if (iter == null) {
            return Optional.empty();
        }

        T e = null;

        while (iter.hasNext()) {
            if ((e = iter.next()) != null) {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    public static <T> Nullable<T> last(final Iterator<T> iter) {
        if (iter == null || iter.hasNext() == false) {
            return Nullable.empty();
        }

        T e = null;

        while (iter.hasNext()) {
            e = iter.next();
        }

        return Nullable.of(e);
    }

    public static <T> Optional<T> lastNonNull(final Iterator<T> iter) {
        if (iter == null) {
            return Optional.empty();
        }

        T e = null;
        T lastNonNull = null;

        while (iter.hasNext()) {
            if ((e = iter.next()) != null) {
                lastNonNull = e;
            }
        }

        return Optional.ofNullable(lastNonNull);
    }

    /**
     * Note: copied from Google Guava under Apache license v2
     * <br />
     * Calls {@code next()} on {@code iterator}, either {@code numberToAdvance} times
     * or until {@code hasNext()} returns {@code false}, whichever comes first.
     *
     * @return the number of elements the iterator was advanced 
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
     * @param iter
     * @param n
     * @return
     */
    public static <T> ObjIterator<T> skip(final Iterator<T> iter, final long n) {
        N.checkArgNotNegative(n, "n");

        if (iter == null || n == 0) {
            return ObjIterator.of(iter);
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
     * Returns a new {@code Iterator}
     * 
     * @param iter
     * @param count
     * @return
     */
    public static <T> ObjIterator<T> limit(final Iterator<T> iter, final long count) {
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
     * @param iter
     * @param offset
     * @param count
     * @return
     */
    public static <T> ObjIterator<T> limit(final Iterator<T> iter, final long offset, final long count) {
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
     * @param iter
     * @return
     */
    public static <T> ObjIterator<T> skipNull(final Iterator<T> iter) {
        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<T>() {
            private final Iterator<T> iterator = iter;
            private T next;

            @Override
            public boolean hasNext() {
                if (next == null && iterator.hasNext()) {
                    next = iterator.next();

                    if (next == null) {
                        while (iterator.hasNext()) {
                            next = iterator.next();

                            if (next != null) {
                                break;
                            }
                        }
                    }
                }

                return next != null;
            }

            @Override
            public T next() {
                if (next == null && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final T result = next;
                next = null;
                return result;
            }
        };
    }

    /**
     * 
     * @param iter
     * @return
     * throws DuplicatedResultException if there are more than one elements in the specified {@code iter}.
     */
    public static <T> Nullable<T> getOnlyElement(final Iterator<? extends T> iter) throws DuplicatedResultException {
        if (iter == null) {
            return Nullable.empty();
        }

        final T first = iter.next();

        if (iter.hasNext()) {
            throw new DuplicatedResultException("Expected at most one element but was: [" + StringUtil.concat(first, ", ", iter.next(), "...]"));
        }

        return Nullable.of(first);
    }

    public static <T> ObjIterator<T> filter(final Iterator<T> iter, final Predicate<? super T> filter) {
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

    public static <T, U> ObjIterator<U> map(final Iterator<T> iter, final Function<? super T, U> mapper) {
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

    public static <T, U> ObjIterator<U> flatMap(final Iterator<T> iter, final Function<? super T, ? extends Collection<? extends U>> mapper) {
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

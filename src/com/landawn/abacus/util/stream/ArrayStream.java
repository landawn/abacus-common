/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executor;

import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.StringUtil.Strings;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;

/**
 *
 * @param <T> 
 */
class ArrayStream<T> extends AbstractStream<T> {
    final T[] elements;
    final int fromIndex;
    final int toIndex;

    ArrayStream(final T[] values) {
        this(values, 0, values.length);
    }

    ArrayStream(final T[] values, final Collection<Runnable> closeHandlers) {
        this(values, 0, values.length, closeHandlers);
    }

    ArrayStream(final T[] values, final boolean sorted, final Comparator<? super T> comparator, final Collection<Runnable> closeHandlers) {
        this(values, 0, values.length, sorted, comparator, closeHandlers);
    }

    ArrayStream(final T[] values, final int fromIndex, final int toIndex) {
        this(values, fromIndex, toIndex, null);
    }

    ArrayStream(final T[] values, final int fromIndex, final int toIndex, final Collection<Runnable> closeHandlers) {
        this(values, fromIndex, toIndex, false, null, closeHandlers);
    }

    ArrayStream(final T[] values, final int fromIndex, final int toIndex, final boolean sorted, Comparator<? super T> comparator,
            final Collection<Runnable> closeHandlers) {
        super(sorted, comparator, closeHandlers);

        checkFromToIndex(fromIndex, toIndex, N.len(values));

        this.elements = values;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    @Override
    public Stream<T> filter(final Predicate<? super T> predicate) {
        return newStream(new ObjIteratorEx<T>() {
            private boolean hasNext = false;
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                if (hasNext == false && cursor < toIndex) {
                    do {
                        if (predicate.test(elements[cursor])) {
                            hasNext = true;
                            break;
                        }
                    } while (++cursor < toIndex);
                }

                return hasNext;
            }

            @Override
            public T next() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted, cmp);
    }

    @Override
    public Stream<T> takeWhile(final Predicate<? super T> predicate) {
        return newStream(new ObjIteratorEx<T>() {
            private boolean hasMore = true;
            private boolean hasNext = false;
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                if (hasNext == false && hasMore && cursor < toIndex) {
                    if (predicate.test(elements[cursor])) {
                        hasNext = true;
                    } else {
                        hasMore = false;
                    }
                }

                return hasNext;
            }

            @Override
            public T next() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted, cmp);
    }

    @Override
    public Stream<T> dropWhile(final Predicate<? super T> predicate) {
        return newStream(new ObjIteratorEx<T>() {
            private boolean hasNext = false;
            private int cursor = fromIndex;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (hasNext == false && cursor < toIndex) {
                    if (dropped == false) {
                        dropped = true;

                        do {
                            if (predicate.test(elements[cursor]) == false) {
                                hasNext = true;
                                break;
                            }
                        } while (++cursor < toIndex);
                    } else {
                        hasNext = true;
                    }
                }

                return hasNext;
            }

            @Override
            public T next() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted, cmp);
    }

    @Override
    public Stream<T> step(final long step) {
        checkArgPositive(step, "step");

        if (step == 1 || fromIndex == toIndex) {
            return newStream(elements, fromIndex, toIndex, sorted, cmp);
        }

        return newStream(new ObjIteratorEx<T>() {
            private final int stepp = (int) N.min(step, Integer.MAX_VALUE);
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

                final T res = elements[cursor];
                cursor = cursor > toIndex - stepp ? toIndex : cursor + stepp;
                return res;
            }

            @Override
            public long count() {
                return (toIndex - cursor) % stepp == 0 ? (toIndex - cursor) / stepp : ((toIndex - cursor) / stepp) + 1;
            }

            @Override
            public void skip(long n) {
                cursor = n <= (toIndex - cursor) / stepp ? cursor + (int) (n * stepp) : toIndex;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                final int len = (int) count();
                a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);

                for (int i = 0; i < len; i++, cursor += stepp) {
                    a[i] = (A) elements[cursor];
                }

                return a;
            }
        }, sorted, cmp);
    }

    @Override
    public <R> Stream<R> map(final Function<? super T, ? extends R> mapper) {
        return newStream(new ObjIteratorEx<R>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public R next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.apply(elements[cursor++]);
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= toIndex - cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = (A) mapper.apply(elements[cursor++]);
                }

                return a;
            }
        }, false, null);
    }

    //    @Override
    //    public <R> Stream<R> biMap(final BiFunction<? super T, ? super T, ? extends R> mapper, final boolean ignoreNotPaired) {
    //        return newStream(new ObjIteratorEx<R>() {
    //            private final int atLeast = ignoreNotPaired ? 2 : 1;
    //            private int cursor = fromIndex;
    //
    //            @Override
    //            public boolean hasNext() {
    //                return toIndex - cursor >= atLeast;
    //            }
    //
    //            @Override
    //            public R next() {
    //                if (toIndex - cursor < atLeast) {
    //                    throw new NoSuchElementException();
    //                }
    //
    //                return mapper.apply(elements[cursor++], cursor == toIndex ? null : elements[cursor++]);
    //            }
    //
    //            //            @Override
    //            //            public long count() {
    //            //                return (toIndex - cursor) / 2 + (ignoreNotPaired || (toIndex - cursor) % 2 == 0 ? 0 : 1);
    //            //            }
    //            //
    //            //            @Override
    //            //            public void skip(long n) {
    //            //                checkArgNotNegative(n, "n");
    //            //                
    //            //                cursor = n < count() ? cursor + (int) n * 2 : toIndex;
    //            //            }
    //
    //            @Override
    //            public <A> A[] toArray(A[] a) {
    //                final int len = (int) count();
    //                a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);
    //
    //                for (int i = 0, len2 = (toIndex - cursor) / 2; i < len2; i++) {
    //                    a[i] = (A) mapper.apply(elements[cursor++], elements[cursor++]);
    //                }
    //
    //                if (cursor < toIndex) {
    //                    a[len - 1] = (A) mapper.apply(elements[cursor++], null);
    //                }
    //
    //                return a;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    @Override
    //    public <R> Stream<R> triMap(final TriFunction<? super T, ? super T, ? super T, ? extends R> mapper, final boolean ignoreNotPaired) {
    //        return newStream(new ObjIteratorEx<R>() {
    //            private final int atLeast = ignoreNotPaired ? 3 : 1;
    //            private int cursor = fromIndex;
    //
    //            @Override
    //            public boolean hasNext() {
    //                return toIndex - cursor >= atLeast;
    //            }
    //
    //            @Override
    //            public R next() {
    //                if (toIndex - cursor < atLeast) {
    //                    throw new NoSuchElementException();
    //                }
    //
    //                return mapper.apply(elements[cursor++], cursor == toIndex ? null : elements[cursor++], cursor == toIndex ? null : elements[cursor++]);
    //            }
    //
    //            //            @Override
    //            //            public long count() {
    //            //                return (toIndex - cursor) / 3 + (ignoreNotPaired || (toIndex - cursor) % 3 == 0 ? 0 : 1);
    //            //            }
    //            //
    //            //            @Override
    //            //            public void skip(long n) {
    //            //                checkArgNotNegative(n, "n");
    //            //                
    //            //                cursor = n < count() ? cursor + (int) n * 3 : toIndex;
    //            //            }
    //
    //            @Override
    //            public <A> A[] toArray(A[] a) {
    //                final int len = (int) count();
    //                a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);
    //
    //                for (int i = 0, len2 = (toIndex - cursor) / 3; i < len2; i++) {
    //                    a[i] = (A) mapper.apply(elements[cursor++], elements[cursor++], elements[cursor++]);
    //                }
    //
    //                if (cursor < toIndex) {
    //                    a[len - 1] = (A) mapper.apply(elements[cursor++], cursor == toIndex ? null : elements[cursor++], null);
    //                }
    //
    //                return a;
    //            }
    //        }, closeHandlers);
    //    }

    @Override
    public <R> Stream<R> slidingMap(final BiFunction<? super T, ? super T, R> mapper, final int increment, final boolean ignoreNotPaired) {
        final int windowSize = 2;

        checkArgPositive(increment, "increment");

        return newStream(new ObjIteratorEx<R>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return ignoreNotPaired ? toIndex - cursor >= windowSize : cursor < toIndex;
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final R result = mapper.apply(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null);

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

            //    @Override
            //    public long count() {
            //        if (toIndex - cursor == 0) {
            //            return 0;
            //        } else if (toIndex - cursor <= windowSize) {
            //            return 1;
            //        } else {
            //            final long len = (toIndex - cursor) - windowSize;
            //            return 1 + (len % increment == 0 ? len / increment : len / increment + 1);
            //        }
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        if (n >= count()) {
            //            cursor = toIndex;
            //        } else {
            //            cursor += n * increment;
            //        }
            //    }
        }, false, null);
    }

    @Override
    public <R> Stream<R> slidingMap(final TriFunction<? super T, ? super T, ? super T, R> mapper, final int increment, final boolean ignoreNotPaired) {
        final int windowSize = 3;

        checkArgPositive(increment, "increment");

        return newStream(new ObjIteratorEx<R>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return ignoreNotPaired ? toIndex - cursor >= windowSize : cursor < toIndex;
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final R result = mapper.apply(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null,
                        cursor < toIndex - 2 ? elements[cursor + 2] : null);

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

            //    @Override
            //    public long count() {
            //        if (toIndex - cursor == 0) {
            //            return 0;
            //        } else if (toIndex - cursor <= windowSize) {
            //            return 1;
            //        } else {
            //            final long len = (toIndex - cursor) - windowSize;
            //            return 1 + (len % increment == 0 ? len / increment : len / increment + 1);
            //        }
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        if (n >= count()) {
            //            cursor = toIndex;
            //        } else {
            //            cursor += n * increment;
            //        }
            //    }
        }, false, null);
    }

    @Override
    public Stream<T> mapFirst(final Function<? super T, ? extends T> mapperForFirst) {
        checkArgNotNull(mapperForFirst);

        if (fromIndex == toIndex) {
            return newStream(elements, fromIndex, toIndex, sorted, cmp);
        } else if (toIndex - fromIndex == 1) {
            return map(mapperForFirst);
        } else {
            return newStream(new ObjIteratorEx<T>() {
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

                    if (cursor == fromIndex) {
                        return mapperForFirst.apply(elements[cursor++]);
                    } else {
                        return elements[cursor++];
                    }
                }

                //    @Override
                //    public long count() {
                //        return toIndex - cursor;
                //    }
                //
                //    @Override
                //    public void skip(long n) {
                //        checkArgNotNegative(n, "n");
                //
                //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
                //    }

                @Override
                public long count() {
                    if (hasNext()) {
                        next();
                        return toIndex - cursor + 1;
                    }

                    return 0;
                }

                @Override
                public void skip(long n) {
                    if (n > 0) {
                        if (hasNext()) {
                            next();
                            n -= 1;
                            cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
                        }
                    }
                }

                @Override
                public <A> A[] toArray(A[] a) {
                    a = a.length >= toIndex - cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - cursor);

                    for (int i = 0, len = toIndex - cursor; i < len; i++) {
                        if (cursor == fromIndex) {
                            a[i] = (A) mapperForFirst.apply(elements[cursor++]);
                        } else {
                            a[i] = (A) elements[cursor++];
                        }
                    }

                    return a;
                }
            }, false, null);
        }
    }

    @Override
    public <R> Stream<R> mapFirstOrElse(final Function<? super T, ? extends R> mapperForFirst, final Function<? super T, ? extends R> mapperForElse) {
        checkArgNotNull(mapperForFirst);
        checkArgNotNull(mapperForElse);

        if (fromIndex == toIndex) {
            return (Stream<R>) this;
        } else if (toIndex - fromIndex == 1) {
            return map(mapperForFirst);
        } else {
            return newStream(new ObjIteratorEx<R>() {
                private int cursor = fromIndex;

                @Override
                public boolean hasNext() {
                    return cursor < toIndex;
                }

                @Override
                public R next() {
                    if (cursor >= toIndex) {
                        throw new NoSuchElementException();
                    }

                    if (cursor == fromIndex) {
                        return mapperForFirst.apply(elements[cursor++]);
                    } else {
                        return mapperForElse.apply(elements[cursor++]);
                    }
                }

                //    @Override
                //    public long count() {
                //        return toIndex - cursor;
                //    }
                //
                //    @Override
                //    public void skip(long n) {
                //        checkArgNotNegative(n, "n");
                //
                //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
                //    }

                @Override
                public <A> A[] toArray(A[] a) {
                    a = a.length >= toIndex - cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - cursor);

                    for (int i = 0, len = toIndex - cursor; i < len; i++) {
                        if (cursor == fromIndex) {
                            a[i] = (A) mapperForFirst.apply(elements[cursor++]);
                        } else {
                            a[i] = (A) mapperForElse.apply(elements[cursor++]);
                        }
                    }

                    return a;
                }
            }, false, null);
        }
    }

    @Override
    public Stream<T> mapLast(final Function<? super T, ? extends T> mapperForLast) {
        checkArgNotNull(mapperForLast);

        if (fromIndex == toIndex) {
            return newStream(elements, fromIndex, toIndex, sorted, cmp);
        } else if (toIndex - fromIndex == 1) {
            return map(mapperForLast);
        } else {
            return newStream(new ObjIteratorEx<T>() {
                private int last = toIndex - 1;
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

                    if (cursor == last) {
                        return mapperForLast.apply(elements[cursor++]);
                    } else {
                        return elements[cursor++];
                    }
                }

                //    @Override
                //    public long count() {
                //        return toIndex - cursor;
                //    }
                //
                //    @Override
                //    public void skip(long n) {
                //        checkArgNotNegative(n, "n");
                //
                //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
                //    }

                @Override
                public <A> A[] toArray(A[] a) {
                    a = a.length >= toIndex - cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - cursor);

                    for (int i = 0, len = toIndex - cursor; i < len; i++) {
                        if (cursor == last) {
                            a[i] = (A) mapperForLast.apply(elements[cursor++]);
                        } else {
                            a[i] = (A) elements[cursor++];
                        }
                    }

                    return a;
                }
            }, false, null);
        }
    }

    @Override
    public <R> Stream<R> mapLastOrElse(final Function<? super T, ? extends R> mapperForLast, final Function<? super T, ? extends R> mapperForElse) {
        checkArgNotNull(mapperForLast);
        checkArgNotNull(mapperForElse);

        return newStream(new ObjIteratorEx<R>() {
            private int last = toIndex - 1;
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public R next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                if (cursor == last) {
                    return mapperForLast.apply(elements[cursor++]);
                } else {
                    return mapperForElse.apply(elements[cursor++]);
                }
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= toIndex - cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    if (cursor == last) {
                        a[i] = (A) mapperForLast.apply(elements[cursor++]);
                    } else {
                        a[i] = (A) mapperForElse.apply(elements[cursor++]);
                    }
                }

                return a;
            }
        }, false, null);
    }

    @Override
    public CharStream mapToChar(final ToCharFunction<? super T> mapper) {
        return newStream(new CharIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public char nextChar() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsChar(elements[cursor++]);
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

            @Override
            public char[] toArray() {
                final char[] a = new char[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsChar(elements[cursor++]);
                }

                return a;
            }
        }, false);
    }

    @Override
    public ByteStream mapToByte(final ToByteFunction<? super T> mapper) {
        return newStream(new ByteIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public byte nextByte() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsByte(elements[cursor++]);
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

            @Override
            public byte[] toArray() {
                final byte[] a = new byte[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsByte(elements[cursor++]);
                }

                return a;
            }
        }, false);
    }

    @Override
    public ShortStream mapToShort(final ToShortFunction<? super T> mapper) {
        return newStream(new ShortIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public short nextShort() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsShort(elements[cursor++]);
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

            @Override
            public short[] toArray() {
                final short[] a = new short[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsShort(elements[cursor++]);
                }

                return a;
            }
        }, false);
    }

    @Override
    public IntStream mapToInt(final ToIntFunction<? super T> mapper) {
        return newStream(new IntIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public int nextInt() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsInt(elements[cursor++]);
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

            @Override
            public int[] toArray() {
                final int[] a = new int[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsInt(elements[cursor++]);
                }

                return a;
            }
        }, false);
    }

    @Override
    public LongStream mapToLong(final ToLongFunction<? super T> mapper) {
        return newStream(new LongIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public long nextLong() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsLong(elements[cursor++]);
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

            @Override
            public long[] toArray() {
                final long[] a = new long[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsLong(elements[cursor++]);
                }

                return a;
            }
        }, false);
    }

    @Override
    public FloatStream mapToFloat(final ToFloatFunction<? super T> mapper) {
        return newStream(new FloatIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public float nextFloat() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsFloat(elements[cursor++]);
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

            @Override
            public float[] toArray() {
                final float[] a = new float[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsFloat(elements[cursor++]);
                }

                return a;
            }
        }, false);
    }

    @Override
    public DoubleStream mapToDouble(final ToDoubleFunction<? super T> mapper) {
        return newStream(new DoubleIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public double nextDouble() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsDouble(elements[cursor++]);
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

            @Override
            public double[] toArray() {
                final double[] a = new double[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsDouble(elements[cursor++]);
                }

                return a;
            }
        }, false);
    }

    @Override
    public <R> Stream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper) {
        final ObjIteratorEx<R> iter = new ObjIteratorEx<R>() {
            private int cursor = fromIndex;
            private Iterator<? extends R> cur = null;
            private Stream<? extends R> s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Deque<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iteratorEx();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public R next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Deque<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalArrayDeque<Runnable>(1)
                : new LocalArrayDeque<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorStream<>(iter, newCloseHandlers);
    }

    @Override
    public CharStream flatMapToChar(final Function<? super T, ? extends CharStream> mapper) {
        final CharIteratorEx iter = new CharIteratorEx() {
            private int cursor = fromIndex;
            private CharIterator cur = null;
            private CharStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Deque<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iteratorEx();
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

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Deque<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalArrayDeque<Runnable>(1)
                : new LocalArrayDeque<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorCharStream(iter, newCloseHandlers);
    }

    @Override
    public ByteStream flatMapToByte(final Function<? super T, ? extends ByteStream> mapper) {
        final ByteIteratorEx iter = new ByteIteratorEx() {
            private int cursor = fromIndex;
            private ByteIterator cur = null;
            private ByteStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Deque<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iteratorEx();
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

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Deque<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalArrayDeque<Runnable>(1)
                : new LocalArrayDeque<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorByteStream(iter, newCloseHandlers);
    }

    @Override
    public ShortStream flatMapToShort(final Function<? super T, ? extends ShortStream> mapper) {
        final ShortIteratorEx iter = new ShortIteratorEx() {
            private int cursor = fromIndex;
            private ShortIterator cur = null;
            private ShortStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Deque<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iteratorEx();
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

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Deque<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalArrayDeque<Runnable>(1)
                : new LocalArrayDeque<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorShortStream(iter, newCloseHandlers);
    }

    @Override
    public IntStream flatMapToInt(final Function<? super T, ? extends IntStream> mapper) {
        final IntIteratorEx iter = new IntIteratorEx() {
            private int cursor = fromIndex;
            private IntIterator cur = null;
            private IntStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Deque<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iteratorEx();
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

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Deque<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalArrayDeque<Runnable>(1)
                : new LocalArrayDeque<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorIntStream(iter, newCloseHandlers);
    }

    @Override
    public LongStream flatMapToLong(final Function<? super T, ? extends LongStream> mapper) {
        final LongIteratorEx iter = new LongIteratorEx() {
            private int cursor = fromIndex;
            private LongIterator cur = null;
            private LongStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Deque<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iteratorEx();
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

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Deque<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalArrayDeque<Runnable>(1)
                : new LocalArrayDeque<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorLongStream(iter, newCloseHandlers);
    }

    @Override
    public FloatStream flatMapToFloat(final Function<? super T, ? extends FloatStream> mapper) {
        final FloatIteratorEx iter = new FloatIteratorEx() {
            private int cursor = fromIndex;
            private FloatIterator cur = null;
            private FloatStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Deque<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iteratorEx();
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

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Deque<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalArrayDeque<Runnable>(1)
                : new LocalArrayDeque<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorFloatStream(iter, newCloseHandlers);
    }

    @Override
    public DoubleStream flatMapToDouble(final Function<? super T, ? extends DoubleStream> mapper) {
        final DoubleIteratorEx iter = new DoubleIteratorEx() {
            private int cursor = fromIndex;
            private DoubleIterator cur = null;
            private DoubleStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Deque<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iteratorEx();
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

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Deque<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalArrayDeque<Runnable>(1)
                : new LocalArrayDeque<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorDoubleStream(iter, newCloseHandlers);
    }

    @Override
    public Stream<Stream<T>> split(final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        return newStream(new ObjIteratorEx<Stream<T>>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return new ArrayStream<>(elements, cursor, (cursor = chunkSize < toIndex - cursor ? cursor + chunkSize : toIndex), sorted, cmp, null);
            }

            @Override
            public long count() {
                final long len = toIndex - cursor;
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void skip(long n) {
                final long len = toIndex - cursor;
                cursor = n <= len / chunkSize ? cursor + (int) n * chunkSize : toIndex;
            }
        }, false, null);
    }

    @Override
    public Stream<List<T>> splitToList(final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        return newStream(new ObjIteratorEx<List<T>>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public List<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return Stream.createList(N.copyOfRange(elements, cursor, (cursor = chunkSize < toIndex - cursor ? cursor + chunkSize : toIndex)));
            }

            @Override
            public long count() {
                final long len = toIndex - cursor;
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void skip(long n) {
                final long len = toIndex - cursor;
                cursor = n <= len / chunkSize ? cursor + (int) n * chunkSize : toIndex;
            }
        }, false, null);
    }

    @Override
    public <C extends Collection<T>> Stream<C> split(final int chunkSize, final IntFunction<? extends C> collectionSupplier) {
        checkArgPositive(chunkSize, "chunkSize");
        checkArgNotNull(collectionSupplier, "collectionSupplier");

        return newStream(new ObjIteratorEx<C>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public C next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final C result = collectionSupplier.apply(toIndex - cursor > chunkSize ? chunkSize : toIndex - cursor);

                for (int to = (cursor = chunkSize < toIndex - cursor ? cursor + chunkSize : toIndex); cursor < to; cursor++) {
                    result.add(elements[cursor]);
                }

                return result;
            }

            @Override
            public long count() {
                final long len = toIndex - cursor;
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void skip(long n) {
                final long len = toIndex - cursor;
                cursor = n <= len / chunkSize ? cursor + (int) n * chunkSize : toIndex;
            }
        }, false, null);
    }

    @Override
    public <A, R> Stream<R> split(final int chunkSize, final Collector<? super T, A, R> collector) {
        checkArgPositive(chunkSize, "chunkSize");
        checkArgNotNull(collector);

        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();

        return newStream(new ObjIteratorEx<R>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public R next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final A container = supplier.get();

                for (int to = (cursor = chunkSize < toIndex - cursor ? cursor + chunkSize : toIndex); cursor < to; cursor++) {
                    accumulator.accept(container, elements[cursor]);
                }

                return finisher.apply(container);
            }

            @Override
            public long count() {
                final long len = toIndex - cursor;
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void skip(long n) {
                final long len = toIndex - cursor;
                cursor = n <= len / chunkSize ? cursor + (int) n * chunkSize : toIndex;
            }
        }, false, null);
    }

    @Override
    public Stream<Stream<T>> split(final Predicate<? super T> predicate) {
        checkArgNotNull(predicate, "predicate");

        return newStream(new ObjIteratorEx<Stream<T>>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final int from = cursor;

                while (cursor < toIndex) {
                    if (from == cursor) {
                        preCondition = predicate.test(elements[cursor]);
                        cursor++;
                    } else if (predicate.test(elements[cursor]) == preCondition) {
                        cursor++;
                    } else {

                        break;
                    }
                }

                return new ArrayStream<>(elements, from, cursor, sorted, cmp, null);
            }
        }, false, null);
    }

    @Override
    public Stream<List<T>> splitToList(final Predicate<? super T> predicate) {
        checkArgNotNull(predicate, "predicate");

        return newStream(new ObjIteratorEx<List<T>>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public List<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final int from = cursor;

                while (cursor < toIndex) {
                    if (from == cursor) {
                        preCondition = predicate.test(elements[cursor]);
                        cursor++;
                    } else if (predicate.test(elements[cursor]) == preCondition) {
                        cursor++;
                    } else {

                        break;
                    }
                }

                return Stream.createList(N.copyOfRange(elements, from, cursor));
            }
        }, false, null);
    }

    @Override
    public <C extends Collection<T>> Stream<C> split(final Predicate<? super T> predicate, final Supplier<? extends C> collectionSupplier) {
        checkArgNotNull(predicate, "predicate");
        checkArgNotNull(collectionSupplier, "collectionSupplier");

        return newStream(new ObjIteratorEx<C>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public C next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final C result = collectionSupplier.get();
                boolean isFirst = true;

                while (cursor < toIndex) {
                    if (isFirst) {
                        preCondition = predicate.test(elements[cursor]);
                        result.add(elements[cursor]);
                        cursor++;
                        isFirst = false;
                    } else if (predicate.test(elements[cursor]) == preCondition) {
                        result.add(elements[cursor]);
                        cursor++;
                    } else {

                        break;
                    }
                }

                return result;
            }
        }, false, null);
    }

    @Override
    public <A, R> Stream<R> split(final Predicate<? super T> predicate, final Collector<? super T, A, R> collector) {
        checkArgNotNull(predicate, "predicate");
        checkArgNotNull(collector);

        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();

        return newStream(new ObjIteratorEx<R>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public R next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final A container = supplier.get();
                boolean isFirst = true;

                while (cursor < toIndex) {
                    if (isFirst) {
                        preCondition = predicate.test(elements[cursor]);
                        accumulator.accept(container, elements[cursor]);
                        cursor++;
                        isFirst = false;
                    } else if (predicate.test(elements[cursor]) == preCondition) {
                        accumulator.accept(container, elements[cursor]);
                        cursor++;
                    } else {

                        break;
                    }
                }

                return finisher.apply(container);
            }
        }, false, null);
    }

    @Override
    public Stream<Stream<T>> splitAt(final int where) {
        checkArgNotNegative(where, "where");

        final Stream<T>[] a = new Stream[2];
        final int middleIndex = where < toIndex - fromIndex ? fromIndex + where : toIndex;
        a[0] = middleIndex == fromIndex ? (Stream<T>) Stream.empty() : new ArrayStream<>(elements, fromIndex, middleIndex, sorted, cmp, null);
        a[1] = middleIndex == toIndex ? (Stream<T>) Stream.empty() : new ArrayStream<>(elements, middleIndex, toIndex, sorted, cmp, null);

        return newStream(a, false, null);
    }

    @Override
    public <A, R> Stream<R> splitAt(final int where, final Collector<? super T, A, R> collector) {
        checkArgNotNegative(where, "where");
        checkArgNotNull(collector, "collector");

        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();

        return newStream(new ObjIteratorEx<R>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public R next() {
                if (hasNext()) {
                    throw new NoSuchElementException();
                }

                final A container = supplier.get();
                final int middleIndex = where < toIndex - fromIndex ? fromIndex + where : toIndex;

                for (int i = cursor == 0 ? fromIndex : middleIndex, to = cursor == 0 ? middleIndex : toIndex; i < to; i++) {
                    accumulator.accept(container, elements[i]);
                }

                cursor++;

                return finisher.apply(container);
            }

            @Override
            public long count() {
                return 2 - cursor;
            }

            @Override
            public void skip(long n) {
                cursor = n >= 2 ? 2 : cursor + (int) n;
            }
        }, false, null);
    }

    @Override
    public Stream<Stream<T>> sliding(final int windowSize, final int increment) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        return newStream(new ObjIteratorEx<Stream<T>>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Stream<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final Stream<T> result = new ArrayStream<>(elements, cursor, windowSize < toIndex - cursor ? cursor + windowSize : toIndex, sorted, cmp, null);

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

            @Override
            public long count() {
                if (toIndex - cursor == 0) {
                    return 0;
                } else if (toIndex - cursor <= windowSize) {
                    return 1;
                } else {
                    final long len = (toIndex - cursor) - windowSize;
                    return 1 + (len % increment == 0 ? len / increment : len / increment + 1);
                }
            }

            @Override
            public void skip(long n) {
                if (n >= count()) {
                    cursor = toIndex;
                } else {
                    cursor += n * increment;
                }
            }
        }, false, null);
    }

    @Override
    public Stream<List<T>> slidingToList(final int windowSize, final int increment) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        return newStream(new ObjIteratorEx<List<T>>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public List<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final List<T> result = Stream.createList(N.copyOfRange(elements, cursor, windowSize < toIndex - cursor ? cursor + windowSize : toIndex));

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

            @Override
            public long count() {
                if (toIndex - cursor == 0) {
                    return 0;
                } else if (toIndex - cursor <= windowSize) {
                    return 1;
                } else {
                    final long len = (toIndex - cursor) - windowSize;
                    return 1 + (len % increment == 0 ? len / increment : len / increment + 1);
                }
            }

            @Override
            public void skip(long n) {
                if (n >= count()) {
                    cursor = toIndex;
                } else {
                    cursor += n * increment;
                }
            }

        }, false, null);
    }

    @Override
    public <C extends Collection<T>> Stream<C> sliding(final int windowSize, final int increment, final IntFunction<? extends C> collectionSupplier) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);
        checkArgNotNull(collectionSupplier, "collectionSupplier");

        return newStream(new ObjIteratorEx<C>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public C next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final C result = collectionSupplier.apply(windowSize < toIndex - cursor ? windowSize : toIndex - cursor);

                for (int i = cursor, to = windowSize < toIndex - cursor ? cursor + windowSize : toIndex; i < to; i++) {
                    result.add(elements[i]);
                }

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

            @Override
            public long count() {
                if (toIndex - cursor == 0) {
                    return 0;
                } else if (toIndex - cursor <= windowSize) {
                    return 1;
                } else {
                    final long len = (toIndex - cursor) - windowSize;
                    return 1 + (len % increment == 0 ? len / increment : len / increment + 1);
                }
            }

            @Override
            public void skip(long n) {
                if (n >= count()) {
                    cursor = toIndex;
                } else {
                    cursor += n * increment;
                }
            }

        }, false, null);
    }

    @Override
    public <A, R> Stream<R> sliding(final int windowSize, final int increment, final Collector<? super T, A, R> collector) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);
        checkArgNotNull(collector);

        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();

        return newStream(new ObjIteratorEx<R>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public R next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final A container = supplier.get();

                for (int i = cursor, to = windowSize < toIndex - cursor ? cursor + windowSize : toIndex; i < to; i++) {
                    accumulator.accept(container, elements[i]);
                }

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return finisher.apply(container);
            }

            @Override
            public long count() {
                if (toIndex - cursor == 0) {
                    return 0;
                } else if (toIndex - cursor <= windowSize) {
                    return 1;
                } else {
                    final long len = (toIndex - cursor) - windowSize;
                    return 1 + (len % increment == 0 ? len / increment : len / increment + 1);
                }
            }

            @Override
            public void skip(long n) {
                if (n >= count()) {
                    cursor = toIndex;
                } else {
                    cursor += n * increment;
                }
            }

        }, false, null);
    }

    @Override
    public Stream<T> top(final int n, final Comparator<? super T> comparator) {
        checkArgPositive(n, "n");

        if (n >= toIndex - fromIndex) {
            return newStream(elements, fromIndex, toIndex, sorted, cmp);
        } else if (sorted && isSameComparator(comparator, cmp)) {
            return newStream(elements, toIndex - n, toIndex, sorted, cmp);
        }

        return newStream(new ObjIteratorEx<T>() {
            private boolean initialized = false;
            private T[] aar;
            private int cursor = 0;
            private int to;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor < to;
            }

            @Override
            public T next() {
                if (initialized == false) {
                    init();
                }

                if (cursor >= to) {
                    throw new NoSuchElementException();
                }

                return aar[cursor++];
            }

            @Override
            public long count() {
                if (initialized == false) {
                    init();
                }

                return to - cursor;
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cursor = n > to - cursor ? to : cursor + (int) n;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                if (initialized == false) {
                    init();
                }

                a = a.length >= (to - cursor) ? a : (A[]) N.newArray(a.getClass().getComponentType(), (to - cursor));

                N.copy(aar, cursor, a, 0, to - cursor);

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = (T[]) N.top(elements, fromIndex, toIndex, n, comparator).toArray();
                    to = aar.length;
                }
            }
        }, false, null);
    }

    @Override
    public Stream<T> peek(final Consumer<? super T> action) {
        return newStream(new ObjIteratorEx<T>() {
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

                action.accept(elements[cursor]);

                return elements[cursor++];
            }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= toIndex - cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    action.accept(elements[cursor]);

                    a[i] = (A) elements[cursor++];
                }

                return a;
            }
        }, sorted, cmp);
    }

    @Override
    public Stream<T> limit(final long maxSize) {
        checkArgNotNegative(maxSize, "maxSize");

        return newStream(elements, fromIndex, (int) (fromIndex + maxSize), sorted, cmp);
    }

    @Override
    public Stream<T> skip(long n) {
        checkArgNotNegative(n, "n");

        if (n >= toIndex - fromIndex) {
            return newStream(elements, toIndex, toIndex, sorted, cmp);
        } else {
            return newStream(elements, (int) (fromIndex + n), toIndex, sorted, cmp);
        }
    }

    @Override
    public <E extends Exception, E2 extends Exception> void forEach(final Try.Consumer<? super T, E> action, final Try.Runnable<E2> onComplete) throws E, E2 {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                action.accept(elements[i]);
            }

            onComplete.run();
        } finally {
            close();
        }
    }

    @Override
    public <U, E extends Exception, E2 extends Exception> void forEach(final Try.Function<? super T, ? extends Collection<U>, E> flatMapper,
            final Try.BiConsumer<? super T, ? super U, E2> action) throws E, E2 {
        assertNotClosed();

        Collection<U> c = null;

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                c = flatMapper.apply(elements[i]);

                if (N.notNullOrEmpty(c)) {
                    for (U u : c) {
                        action.accept(elements[i], u);
                    }
                }
            }
        } finally {
            close();
        }
    }

    @Override
    public <T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEach(
            final Try.Function<? super T, ? extends Collection<T2>, E> flatMapper, final Try.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2,
            final Try.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        assertNotClosed();

        Collection<T2> c2 = null;
        Collection<T3> c3 = null;

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                c2 = flatMapper.apply(elements[i]);

                if (N.notNullOrEmpty(c2)) {
                    for (T2 t2 : c2) {
                        c3 = flatMapper2.apply(t2);

                        if (N.notNullOrEmpty(c3)) {
                            for (T3 t3 : c3) {
                                action.accept(elements[i], t2, t3);
                            }
                        }
                    }
                }
            }
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> void forEachPair(final Try.BiConsumer<? super T, ? super T, E> action, final int increment) throws E {
        final int windowSize = 2;
        checkArgPositive(increment, "increment");
        assertNotClosed();

        try {
            int cursor = fromIndex;

            while (cursor < toIndex) {
                action.accept(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null);

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;
            }
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> void forEachTriple(final Try.TriConsumer<? super T, ? super T, ? super T, E> action, final int increment) throws E {
        final int windowSize = 3;
        checkArgPositive(increment, "increment");
        assertNotClosed();

        try {
            int cursor = fromIndex;

            while (cursor < toIndex) {
                action.accept(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null, cursor < toIndex - 2 ? elements[cursor + 2] : null);

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;
            }
        } finally {
            close();
        }
    }

    @Override
    public Object[] toArray() {
        assertNotClosed();

        try {
            return N.copyOfRange(elements, fromIndex, toIndex);
        } finally {
            close();
        }
    }

    <A> A[] toArray(A[] a) {
        assertNotClosed();

        try {
            if (a.length < (toIndex - fromIndex)) {
                a = N.newArray(a.getClass().getComponentType(), toIndex - fromIndex);
            }

            N.copy(elements, fromIndex, a, 0, toIndex - fromIndex);

            return a;
        } finally {
            close();
        }
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        assertNotClosed();

        try {
            return toArray(generator.apply(toIndex - fromIndex));
        } finally {
            close();
        }
    }

    @Override
    public List<T> toList() {
        assertNotClosed();

        try {
            // return Stream.createList(N.copyOfRange(elements, fromIndex, toIndex));

            if (fromIndex == 0 && toIndex == elements.length && elements.length > 9) {
                return new ArrayList<>(Arrays.asList(elements));
            }

            final List<T> result = new ArrayList<>(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Set<T> toSet() {
        assertNotClosed();

        try {
            final Set<T> result = new HashSet<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <C extends Collection<T>> C toCollection(Supplier<? extends C> supplier) {
        assertNotClosed();

        try {
            final C result = supplier.get();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<T> toMultiset() {
        assertNotClosed();

        try {
            final Multiset<T> result = new Multiset<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<T> toMultiset(Supplier<? extends Multiset<T>> supplier) {
        assertNotClosed();

        try {
            final Multiset<T> result = supplier.get();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public LongMultiset<T> toLongMultiset() {
        assertNotClosed();

        try {
            final LongMultiset<T> result = new LongMultiset<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public LongMultiset<T> toLongMultiset(Supplier<? extends LongMultiset<T>> supplier) {
        assertNotClosed();

        try {
            final LongMultiset<T> result = supplier.get();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();

            for (int i = fromIndex; i < toIndex; i++) {
                Collectors.merge(result, keyMapper.apply(elements[i]), valueMapper.apply(elements[i]), mergeFunction);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, A, D, M extends Map<K, D>> M toMap(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Collector<? super V, A, D> downstream, final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            final Supplier<A> downstreamSupplier = downstream.supplier();
            final BiConsumer<A, ? super V> downstreamAccumulator = downstream.accumulator();
            final Function<A, D> downstreamFinisher = downstream.finisher();
            final Map<K, A> intermediate = (Map<K, A>) result;
            K key = null;
            A v = null;

            for (int i = fromIndex; i < toIndex; i++) {
                key = checkArgNotNull(keyMapper.apply(elements[i]), "element cannot be mapped to a null key");

                if ((v = intermediate.get(key)) == null) {
                    if ((v = downstreamSupplier.get()) != null) {
                        intermediate.put(key, v);
                    }
                }

                downstreamAccumulator.accept(v, valueMapper.apply(elements[i]));
            }

            final BiFunction<? super K, ? super A, ? extends A> function = new BiFunction<K, A, A>() {
                @Override
                public A apply(K k, A v) {
                    return (A) downstreamFinisher.apply(v);
                }
            };

            Collectors.replaceAll(intermediate, function);

            return result;
        } finally {
            close();
        }
    }

    //    @Override
    //    public <K, V, M extends Map<K, V>> M flatToMap(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) {
    //        assertNotClosed();
    //
    //        try {
    //            final M result = mapFactory.get();
    //            ObjIterator<? extends K> keyIter = null;
    //            K k = null;
    //
    //            for (int i = fromIndex; i < toIndex; i++) {
    //                try (Stream<? extends K> ks = flatKeyMapper.apply(elements[i])) {
    //                    keyIter = ks.iterator();
    //
    //                    while (keyIter.hasNext()) {
    //                        k = keyIter.next();
    //                        Collectors.merge(result, k, valueMapper.apply(k, elements[i]), mergeFunction);
    //                    }
    //                }
    //            }
    //
    //            return result;
    //        } finally {
    //            close();
    //        }
    //    }
    //
    //    @Override
    //    public <K, V, A, D, M extends Map<K, D>> M flatToMap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Collector<? super V, A, D> downstream, Supplier<? extends M> mapFactory) {
    //        assertNotClosed();
    //
    //        try {
    //            final M result = mapFactory.get();
    //            final Supplier<A> downstreamSupplier = downstream.supplier();
    //            final BiConsumer<A, ? super V> downstreamAccumulator = downstream.accumulator();
    //            final Function<A, D> downstreamFinisher = downstream.finisher();
    //            final Map<K, A> intermediate = (Map<K, A>) result;
    //
    //            ObjIterator<? extends K> keyIter = null;
    //            K k = null;
    //            A v = null;
    //
    //            for (int i = fromIndex; i < toIndex; i++) {
    //
    //                try (Stream<? extends K> ks = flatKeyMapper.apply(elements[i])) {
    //                    keyIter = ks.iterator();
    //
    //                    while (keyIter.hasNext()) {
    //                        k = checkArgNotNull(keyIter.next(), "element cannot be mapped to a null key");
    //
    //                        if ((v = intermediate.get(k)) == null) {
    //                            if ((v = downstreamSupplier.get()) != null) {
    //                                intermediate.put(k, v);
    //                            }
    //                        }
    //
    //                        downstreamAccumulator.accept(v, valueMapper.apply(k, elements[i]));
    //                    }
    //                }
    //            }
    //
    //            final BiFunction<? super K, ? super A, ? extends A> function = new BiFunction<K, A, A>() {
    //                @Override
    //                public A apply(K k, A v) {
    //                    return (A) downstreamFinisher.apply(v);
    //                }
    //            };
    //
    //            Collectors.replaceAll(intermediate, function);
    //
    //            return result;
    //        } finally {
    //            close();
    //        }
    //    }
    //
    //    @Override
    //    public <K, V, M extends Map<K, V>> M flattToMap(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) {
    //        assertNotClosed();
    //
    //        try {
    //            final M result = mapFactory.get();
    //            Collection<? extends K> ks = null;
    //
    //            for (int i = fromIndex; i < toIndex; i++) {
    //                ks = flatKeyMapper.apply(elements[i]);
    //
    //                if (N.notNullOrEmpty(ks)) {
    //                    for (K k : ks) {
    //                        Collectors.merge(result, k, valueMapper.apply(k, elements[i]), mergeFunction);
    //                    }
    //                }
    //            }
    //
    //            return result;
    //        } finally {
    //            close();
    //        }
    //    }
    //
    //    @Override
    //    public <K, V, A, D, M extends Map<K, D>> M flattToMap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Collector<? super V, A, D> downstream, Supplier<? extends M> mapFactory) {
    //        assertNotClosed();
    //
    //        try {
    //            final M result = mapFactory.get();
    //            final Supplier<A> downstreamSupplier = downstream.supplier();
    //            final BiConsumer<A, ? super V> downstreamAccumulator = downstream.accumulator();
    //            final Function<A, D> downstreamFinisher = downstream.finisher();
    //            final Map<K, A> intermediate = (Map<K, A>) result;
    //
    //            Collection<? extends K> ks = null;
    //            A v = null;
    //
    //            for (int i = fromIndex; i < toIndex; i++) {
    //                ks = flatKeyMapper.apply(elements[i]);
    //
    //                if (N.notNullOrEmpty(ks)) {
    //                    for (K k : ks) {
    //                        checkArgNotNull(k, "element cannot be mapped to a null key");
    //
    //                        if ((v = intermediate.get(k)) == null) {
    //                            if ((v = downstreamSupplier.get()) != null) {
    //                                intermediate.put(k, v);
    //                            }
    //                        }
    //
    //                        downstreamAccumulator.accept(v, valueMapper.apply(k, elements[i]));
    //                    }
    //                }
    //            }
    //
    //            final BiFunction<? super K, ? super A, ? extends A> function = new BiFunction<K, A, A>() {
    //                @Override
    //                public A apply(K k, A v) {
    //                    return (A) downstreamFinisher.apply(v);
    //                }
    //            };
    //
    //            Collectors.replaceAll(intermediate, function);
    //
    //            return result;
    //        } finally {
    //            close();
    //        }
    //    }

    @Override
    public <K, V, C extends Collection<V>, M extends Multimap<K, V, C>> M toMultimap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();

            for (int i = fromIndex; i < toIndex; i++) {
                result.put(keyMapper.apply(elements[i]), valueMapper.apply(elements[i]));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, C extends Collection<V>, M extends Multimap<K, V, C>> M flatToMultimap(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            ObjIterator<? extends K> keyIter = null;
            K k = null;

            for (int i = fromIndex; i < toIndex; i++) {
                try (Stream<? extends K> ks = flatKeyMapper.apply(elements[i])) {
                    keyIter = ks.iteratorEx();

                    while (keyIter.hasNext()) {
                        k = keyIter.next();
                        result.put(k, valueMapper.apply(k, elements[i]));
                    }
                }
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, C extends Collection<V>, M extends Multimap<K, V, C>> M flattToMultimap(
            final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper, final BiFunction<? super K, ? super T, ? extends V> valueMapper,
            final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();

            Collection<? extends K> ks = null;

            for (int i = fromIndex; i < toIndex; i++) {
                ks = flatKeyMapper.apply(elements[i]);

                if (N.notNullOrEmpty(ks)) {
                    for (K k : ks) {
                        result.put(k, valueMapper.apply(k, elements[i]));
                    }
                }
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> first() {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? Optional.of(elements[fromIndex]) : Optional.<T> empty();
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> last() {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? Optional.of(elements[toIndex - 1]) : Optional.<T> empty();
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> onlyOne() throws DuplicatedResultException {
        assertNotClosed();

        try {
            final int size = toIndex - fromIndex;

            if (size == 0) {
                return Optional.empty();
            } else if (size == 1) {
                return Optional.of(elements[fromIndex]);
            } else {
                throw new DuplicatedResultException("There are at least two elements: " + Strings.concat(elements[fromIndex], ", ", elements[fromIndex + 1]));
            }
        } finally {
            close();
        }
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        assertNotClosed();

        try {
            T result = identity;

            for (int i = fromIndex; i < toIndex; i++) {
                result = accumulator.apply(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return Optional.empty();
            }

            T result = elements[fromIndex];

            for (int i = fromIndex + 1; i < toIndex; i++) {
                result = accumulator.apply(result, elements[i]);
            }

            return Optional.of(result);
        } finally {
            close();
        }
    }

    @Override
    public <U> U reduce(final U identity, final BiFunction<U, ? super T, U> accumulator, final BinaryOperator<U> combiner) {
        assertNotClosed();

        try {
            U result = identity;

            for (int i = fromIndex; i < toIndex; i++) {
                result = accumulator.apply(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<? super R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        assertNotClosed();

        try {
            final R result = supplier.get();

            for (int i = fromIndex; i < toIndex; i++) {
                accumulator.accept(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        assertNotClosed();

        try {
            final A container = collector.supplier().get();
            final BiConsumer<A, ? super T> accumulator = collector.accumulator();

            for (int i = fromIndex; i < toIndex; i++) {
                accumulator.accept(container, elements[i]);
            }

            return collector.finisher().apply(container);
        } finally {
            close();
        }
    }

    @Override
    public Stream<T> last(final int n) {
        checkArgNotNegative(n, "n");

        try {
            if (toIndex - fromIndex <= n) {
                return newStream(elements, fromIndex, toIndex, sorted, cmp);
            }

            return newStream(elements, toIndex - n, toIndex, sorted, cmp);
        } finally {
            close();
        }
    }

    @Override
    public Stream<T> skipLast(int n) {
        if (n <= 0) {
            return newStream(elements, fromIndex, toIndex, sorted, cmp);
        }

        return newStream(elements, fromIndex, N.max(fromIndex, toIndex - n), sorted, cmp);
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return Optional.empty();
            } else if (sorted && isSameComparator(cmp, comparator)) {
                return Optional.of(elements[fromIndex]);
            }

            return Optional.of(N.min(elements, fromIndex, toIndex, comparator));
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return Optional.empty();
            } else if (sorted && isSameComparator(cmp, comparator)) {
                return Optional.of(elements[toIndex - 1]);
            }

            return Optional.of(N.max(elements, fromIndex, toIndex, comparator));
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> kthLargest(int k, Comparator<? super T> comparator) {
        checkArgPositive(k, "k");
        assertNotClosed();

        try {
            if (k > toIndex - fromIndex) {
                return Optional.empty();
            } else if (sorted && isSameComparator(cmp, comparator)) {
                return Optional.of(elements[toIndex - k]);
            }

            return Optional.of(N.kthLargest(elements, fromIndex, toIndex, k, comparator));
        } finally {
            close();
        }
    }

    @Override
    public long count() {
        assertNotClosed();

        try {
            return toIndex - fromIndex;
        } finally {
            close();
        }
    }

    @Override
    public Stream<T> reversed() {
        return newStream(new ObjIteratorEx<T>() {
            private int cursor = toIndex;

            @Override
            public boolean hasNext() {
                return cursor > fromIndex;
            }

            @Override
            public T next() {
                if (cursor <= fromIndex) {
                    throw new NoSuchElementException();
                }

                return elements[--cursor];
            }

            @Override
            public long count() {
                return cursor - fromIndex;
            }

            @Override
            public void skip(long n) {
                cursor = n < cursor - fromIndex ? cursor - (int) n : fromIndex;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= cursor - fromIndex ? a : (A[]) N.newArray(a.getClass().getComponentType(), cursor - fromIndex);

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = (A) elements[cursor - i - 1];
                }

                return a;
            }
        }, false, null);
    }

    @Override
    public Stream<T> rotated(final int distance) {
        if (distance == 0 || toIndex - fromIndex <= 1 || distance % (toIndex - fromIndex) == 0) {
            return newStream(elements, fromIndex, toIndex, sorted, cmp);
        }

        return newStream(new ObjIteratorEx<T>() {
            private final int len = toIndex - fromIndex;
            private int start;
            private int cnt = 0;

            {

                start = distance % len;

                if (start < 0) {
                    start += len;
                }

                start = len - start;
            }

            @Override
            public boolean hasNext() {
                return cnt < len;
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return elements[((start + cnt++) % len) + fromIndex];
            }

            @Override
            public long count() {
                return len - cnt;
            }

            @Override
            public void skip(long n) {
                cnt = n < len - cnt ? cnt + (int) n : len;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= len - cnt ? a : (A[]) N.newArray(a.getClass().getComponentType(), len - cnt);

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = (A) elements[((start + i) % len) + fromIndex];
                }

                return a;
            }
        }, false, null);
    }

    @Override
    public <E extends Exception> boolean anyMatch(final Try.Predicate<? super T, E> predicate) throws E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return true;
                }
            }

            return false;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> boolean allMatch(final Try.Predicate<? super T, E> predicate) throws E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i]) == false) {
                    return false;
                }
            }

            return true;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> boolean noneMatch(final Try.Predicate<? super T, E> predicate) throws E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return false;
                }
            }

            return true;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> boolean nMatch(final long atLeast, final long atMost, Try.Predicate<? super T, E> predicate) throws E {
        checkArgNotNegative(atLeast, "atLeast");
        checkArgNotNegative(atMost, "atMost");
        checkArgument(atLeast <= atMost, "'atLeast' must be <= 'atMost'");

        assertNotClosed();

        long cnt = 0;

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    if (++cnt > atMost) {
                        return false;
                    }
                }
            }
        } finally {
            close();
        }

        return cnt >= atLeast && cnt <= atMost;
    }

    @Override
    public <E extends Exception> Optional<T> findFirst(final Try.Predicate<? super T, E> predicate) throws E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return Optional.of(elements[i]);
                }
            }

            return (Optional<T>) Optional.empty();
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> Optional<T> findLast(final Try.Predicate<? super T, E> predicate) throws E {
        assertNotClosed();

        try {
            for (int i = toIndex - 1; i >= fromIndex; i--) {
                if (predicate.test(elements[i])) {
                    return Optional.of(elements[i]);
                }
            }

            return (Optional<T>) Optional.empty();
        } finally {
            close();
        }
    }

    @Override
    public Stream<T> appendIfEmpty(Collection<? extends T> c) {
        return fromIndex == toIndex ? append(c) : this;
    }

    @Override
    public Stream<T> appendIfEmpty(final Supplier<Stream<T>> supplier) {
        if (fromIndex == toIndex) {
            return append(supplier.get());
        } else {
            return this;
        }
    }

    @Override
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(final Try.Function<? super Stream<T>, R, E> func) throws E {
        try {
            if (fromIndex < toIndex) {
                return Optional.of(func.apply(this));
            } else {
                return Optional.empty();
            }
        } finally {
            if (isClosed == false) {
                close();
            }
        }
    }

    @Override
    public <E extends Exception> void acceptIfNotEmpty(Try.Consumer<? super Stream<T>, E> action) throws E {
        try {
            if (fromIndex < toIndex) {
                action.accept(this);
            }
        } finally {
            if (isClosed == false) {
                close();
            }
        }
    }

    //    @Override
    //    public Stream<T> cached() {
    //        return this;
    //    }

    /**
     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterator asynchronously.
     * 
     * @param stream
     * @param queueSize Default value is 8
     * @return
     */
    @Override
    public Stream<T> queued(int queueSize) {
        return this;
    }

    @Override
    ObjIteratorEx<T> iteratorEx() {
        return ObjIteratorEx.of(elements, fromIndex, toIndex);
    }

    @Override
    public Stream<T> parallel(final int maxThreadNum, final Splitor splitor) {
        return new ParallelArrayStream<>(elements, fromIndex, toIndex, sorted, cmp, checkMaxThreadNum(maxThreadNum), checkSplitor(splitor), asyncExecutor(),
                closeHandlers);
    }

    @Override
    public Stream<T> parallel(final int maxThreadNum, final Executor executor) {
        return new ParallelArrayStream<>(elements, fromIndex, toIndex, sorted, cmp, checkMaxThreadNum(maxThreadNum), splitor(), createAsyncExecutor(executor),
                closeHandlers);
    }

    @Override
    public java.util.stream.Stream<T> toJdkStream() {
        java.util.stream.Stream<T> s = java.util.stream.Stream.of(elements);

        if (fromIndex > 0) {
            s = s.skip(fromIndex);
        }

        if (toIndex < elements.length) {
            s = s.limit(toIndex - fromIndex);
        }

        if (this.isParallel()) {
            s = s.parallel();
        }

        if (N.notNullOrEmpty(closeHandlers)) {
            s = s.onClose(() -> close(closeHandlers));
        }

        return s;
    }

    @Override
    public Stream<T> onClose(Runnable closeHandler) {
        final Deque<Runnable> newCloseHandlers = new LocalArrayDeque<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        newCloseHandlers.add(wrapCloseHandlers(closeHandler));

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        return new ArrayStream<>(elements, fromIndex, toIndex, sorted, cmp, newCloseHandlers);
    }
}

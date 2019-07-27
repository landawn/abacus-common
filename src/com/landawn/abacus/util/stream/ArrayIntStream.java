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
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.IntSummaryStatistics;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.StringUtil.Strings;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.IntBinaryOperator;
import com.landawn.abacus.util.function.IntConsumer;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.IntToByteFunction;
import com.landawn.abacus.util.function.IntToCharFunction;
import com.landawn.abacus.util.function.IntToDoubleFunction;
import com.landawn.abacus.util.function.IntToFloatFunction;
import com.landawn.abacus.util.function.IntToLongFunction;
import com.landawn.abacus.util.function.IntToShortFunction;
import com.landawn.abacus.util.function.IntUnaryOperator;
import com.landawn.abacus.util.function.ObjIntConsumer;
import com.landawn.abacus.util.function.Supplier;

/**
 * 
 */
class ArrayIntStream extends AbstractIntStream {
    final int[] elements;
    final int fromIndex;
    final int toIndex;

    ArrayIntStream(final int[] values) {
        this(values, 0, values.length);
    }

    ArrayIntStream(final int[] values, final Collection<Runnable> closeHandlers) {
        this(values, 0, values.length, closeHandlers);
    }

    ArrayIntStream(final int[] values, final boolean sorted, final Collection<Runnable> closeHandlers) {
        this(values, 0, values.length, sorted, closeHandlers);
    }

    ArrayIntStream(final int[] values, final int fromIndex, final int toIndex) {
        this(values, fromIndex, toIndex, null);
    }

    ArrayIntStream(final int[] values, final int fromIndex, final int toIndex, final Collection<Runnable> closeHandlers) {
        this(values, fromIndex, toIndex, false, closeHandlers);
    }

    ArrayIntStream(final int[] values, final int fromIndex, final int toIndex, final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, closeHandlers);

        checkFromToIndex(fromIndex, toIndex, N.len(values));

        this.elements = values;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    @Override
    public IntStream filter(final IntPredicate predicate) {
        return newStream(new IntIteratorEx() {
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
            public int nextInt() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted);
    }

    @Override
    public IntStream takeWhile(final IntPredicate predicate) {
        return newStream(new IntIteratorEx() {
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
            public int nextInt() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted);
    }

    @Override
    public IntStream dropWhile(final IntPredicate predicate) {
        return newStream(new IntIteratorEx() {
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
            public int nextInt() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted);
    }

    @Override
    public IntStream step(final long step) {
        checkArgPositive(step, "step");

        if (step == 1 || fromIndex == toIndex) {
            return newStream(elements, fromIndex, toIndex, sorted);
        }

        return newStream(new IntIteratorEx() {
            private final int stepp = (int) N.min(step, Integer.MAX_VALUE);
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

                final int res = elements[cursor];
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
            public int[] toArray() {
                final int[] a = new int[(int) count()];

                for (int i = 0, len = a.length; i < len; i++, cursor += stepp) {
                    a[i] = elements[cursor];
                }

                return a;
            }
        }, sorted);
    }

    @Override
    public IntStream map(final IntUnaryOperator mapper) {
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
    public CharStream mapToChar(final IntToCharFunction mapper) {
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
    public ByteStream mapToByte(final IntToByteFunction mapper) {
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
    public ShortStream mapToShort(final IntToShortFunction mapper) {
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
    public LongStream mapToLong(final IntToLongFunction mapper) {
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
    public FloatStream mapToFloat(final IntToFloatFunction mapper) {
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
    public DoubleStream mapToDouble(final IntToDoubleFunction mapper) {
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
    public <U> Stream<U> mapToObj(final IntFunction<? extends U> mapper) {
        return newStream(new ObjIteratorEx<U>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public U next() {
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

    @Override
    public IntStream flatMap(final IntFunction<? extends IntStream> mapper) {
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
    public CharStream flatMapToChar(final IntFunction<? extends CharStream> mapper) {
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
    public ByteStream flatMapToByte(final IntFunction<? extends ByteStream> mapper) {
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
    public ShortStream flatMapToShort(final IntFunction<? extends ShortStream> mapper) {
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
    public LongStream flatMapToLong(final IntFunction<? extends LongStream> mapper) {
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
    public FloatStream flatMapToFloat(final IntFunction<? extends FloatStream> mapper) {
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
    public DoubleStream flatMapToDouble(final IntFunction<? extends DoubleStream> mapper) {
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
    public <T> Stream<T> flatMapToObj(final IntFunction<? extends Stream<T>> mapper) {
        final ObjIteratorEx<T> iter = new ObjIteratorEx<T>() {
            private int cursor = fromIndex;
            private Iterator<T> cur = null;
            private Stream<T> s = null;
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
            public T next() {
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
    public Stream<IntStream> split(final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        return newStream(new ObjIteratorEx<IntStream>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public IntStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return new ArrayIntStream(elements, cursor, (cursor = chunkSize < toIndex - cursor ? cursor + chunkSize : toIndex), sorted, null);
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
    public Stream<IntList> splitToList(final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        return newStream(new ObjIteratorEx<IntList>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public IntList next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return new IntList(N.copyOfRange(elements, cursor, (cursor = chunkSize < toIndex - cursor ? cursor + chunkSize : toIndex)));
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
    public Stream<IntStream> split(final IntPredicate predicate) {
        return newStream(new ObjIteratorEx<IntStream>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public IntStream next() {
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

                return new ArrayIntStream(elements, from, cursor, sorted, null);
            }
        }, false, null);
    }

    @Override
    public Stream<IntList> splitToList(final IntPredicate predicate) {
        return newStream(new ObjIteratorEx<IntList>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public IntList next() {
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

                return new IntList(N.copyOfRange(elements, from, cursor));
            }
        }, false, null);
    }

    @Override
    public Stream<IntStream> splitAt(final int where) {
        checkArgNotNegative(where, "where");

        final IntStream[] a = new IntStream[2];
        final int middleIndex = where < toIndex - fromIndex ? fromIndex + where : toIndex;
        a[0] = middleIndex == fromIndex ? IntStream.empty() : new ArrayIntStream(elements, fromIndex, middleIndex, sorted, null);
        a[1] = middleIndex == toIndex ? IntStream.empty() : new ArrayIntStream(elements, middleIndex, toIndex, sorted, null);

        return newStream(a, false, null);
    }

    @Override
    public Stream<IntStream> sliding(final int windowSize, final int increment) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        return newStream(new ObjIteratorEx<IntStream>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public IntStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final ArrayIntStream result = new ArrayIntStream(elements, cursor, windowSize < toIndex - cursor ? cursor + windowSize : toIndex, sorted, null);

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
    public Stream<IntList> slidingToList(final int windowSize, final int increment) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        return newStream(new ObjIteratorEx<IntList>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public IntList next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final IntList result = IntList.of(N.copyOfRange(elements, cursor, windowSize < toIndex - cursor ? cursor + windowSize : toIndex));

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
    public IntStream top(final int n, final Comparator<? super Integer> comparator) {
        checkArgPositive(n, "n");

        if (n >= toIndex - fromIndex) {
            return newStream(elements, fromIndex, toIndex, sorted);
        } else if (sorted && isSameComparator(comparator, cmp)) {
            return newStream(elements, toIndex - n, toIndex, sorted);
        }

        return newStream(new IntIteratorEx() {
            private boolean initialized = false;
            private int[] aar;
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
            public int nextInt() {
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
            public int[] toArray() {
                if (initialized == false) {
                    init();
                }

                final int[] a = new int[to - cursor];

                N.copy(aar, cursor, a, 0, to - cursor);

                return a;
            }

            @Override
            public IntList toList() {
                return IntList.of(toArray());
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = N.top(elements, fromIndex, toIndex, n, comparator);
                    to = aar.length;
                }
            }
        }, false);
    }

    @Override
    public IntStream peek(final IntConsumer action) {
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

                action.accept(elements[cursor]);

                return elements[cursor++];
            }

            @Override
            public int[] toArray() {
                final int[] a = new int[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    action.accept(elements[cursor]);

                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, sorted);
    }

    @Override
    public IntStream limit(final long maxSize) {
        checkArgNotNegative(maxSize, "maxSize");

        return newStream(elements, fromIndex, (int) (fromIndex + maxSize), sorted);
    }

    @Override
    public IntStream skip(long n) {
        checkArgNotNegative(n, "n");

        if (n >= toIndex - fromIndex) {
            return newStream(elements, toIndex, toIndex, sorted);
        } else {
            return newStream(elements, (int) (fromIndex + n), toIndex, sorted);
        }
    }

    @Override
    public <E extends Exception> void forEach(final Try.IntConsumer<E> action) throws E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                action.accept(elements[i]);
            }
        } finally {
            close();
        }
    }

    @Override
    public int[] toArray() {
        assertNotClosed();

        try {
            return N.copyOfRange(elements, fromIndex, toIndex);
        } finally {
            close();
        }
    }

    @Override
    public IntList toIntList() {
        assertNotClosed();

        try {
            return IntList.of(N.copyOfRange(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    @Override
    public List<Integer> toList() {
        assertNotClosed();

        try {
            final List<Integer> result = new ArrayList<>(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Set<Integer> toSet() {
        assertNotClosed();

        try {
            final Set<Integer> result = new HashSet<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <C extends Collection<Integer>> C toCollection(Supplier<? extends C> supplier) {
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
    public Multiset<Integer> toMultiset() {
        assertNotClosed();

        try {
            final Multiset<Integer> result = new Multiset<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<Integer> toMultiset(Supplier<? extends Multiset<Integer>> supplier) {
        assertNotClosed();

        try {
            final Multiset<Integer> result = supplier.get();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public LongMultiset<Integer> toLongMultiset() {
        assertNotClosed();

        try {
            final LongMultiset<Integer> result = new LongMultiset<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public LongMultiset<Integer> toLongMultiset(Supplier<? extends LongMultiset<Integer>> supplier) {
        assertNotClosed();

        try {
            final LongMultiset<Integer> result = supplier.get();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(IntFunction<? extends K> keyMapper, IntFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction,
            Supplier<? extends M> mapFactory) {
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
    public <K, A, D, M extends Map<K, D>> M toMap(final IntFunction<? extends K> keyMapper, final Collector<Integer, A, D> downstream,
            final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            final Supplier<A> downstreamSupplier = downstream.supplier();
            final BiConsumer<A, Integer> downstreamAccumulator = downstream.accumulator();
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

                downstreamAccumulator.accept(v, elements[i]);
            }

            final BiFunction<? super K, ? super A, ? extends A> function = new BiFunction<K, A, A>() {
                @Override
                public A apply(K k, A v) {
                    return (A) downstream.finisher().apply(v);
                }
            };

            Collectors.replaceAll(intermediate, function);

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt first() {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalInt.of(elements[fromIndex]) : OptionalInt.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt last() {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalInt.of(elements[toIndex - 1]) : OptionalInt.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt onlyOne() throws DuplicatedResultException {
        assertNotClosed();

        try {
            final int size = toIndex - fromIndex;

            if (size == 0) {
                return OptionalInt.empty();
            } else if (size == 1) {
                return OptionalInt.of(elements[fromIndex]);
            } else {
                throw new DuplicatedResultException("There are at least two elements: " + Strings.concat(elements[fromIndex], ", ", elements[fromIndex + 1]));
            }
        } finally {
            close();
        }
    }

    @Override
    public int reduce(int identity, IntBinaryOperator op) {
        assertNotClosed();

        try {
            int result = identity;

            for (int i = fromIndex; i < toIndex; i++) {
                result = op.applyAsInt(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt reduce(IntBinaryOperator op) {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalInt.empty();
            }

            int result = elements[fromIndex];

            for (int i = fromIndex + 1; i < toIndex; i++) {
                result = op.applyAsInt(result, elements[i]);
            }

            return OptionalInt.of(result);
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjIntConsumer<? super R> accumulator, BiConsumer<R, R> combiner) {
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
    public OptionalInt min() {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalInt.empty();
            } else if (sorted) {
                return OptionalInt.of(elements[fromIndex]);
            }

            return OptionalInt.of(N.min(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt max() {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalInt.empty();
            } else if (sorted) {
                return OptionalInt.of(elements[toIndex - 1]);
            }

            return OptionalInt.of(N.max(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt kthLargest(int k) {
        checkArgPositive(k, "k");
        assertNotClosed();

        try {
            if (k > toIndex - fromIndex) {
                return OptionalInt.empty();
            } else if (sorted) {
                return OptionalInt.of(elements[toIndex - k]);
            }

            return OptionalInt.of(N.kthLargest(elements, fromIndex, toIndex, k));
        } finally {
            close();
        }
    }

    @Override
    public int sum() {
        assertNotClosed();

        try {
            return sum(elements, fromIndex, toIndex);
        } finally {
            close();
        }
    }

    @Override
    public OptionalDouble average() {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalDouble.empty();
            }

            return OptionalDouble.of(sum() / toIndex - fromIndex);
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
    public IntStream reversed() {
        return newStream(new IntIteratorEx() {
            private int cursor = toIndex;

            @Override
            public boolean hasNext() {
                return cursor > fromIndex;
            }

            @Override
            public int nextInt() {
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
            public int[] toArray() {
                final int[] a = new int[cursor - fromIndex];

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = elements[cursor - i - 1];
                }

                return a;
            }
        }, false);
    }

    @Override
    public IntStream rotated(final int distance) {
        if (distance == 0 || toIndex - fromIndex <= 1 || distance % (toIndex - fromIndex) == 0) {
            return newStream(elements, fromIndex, toIndex, sorted);
        }

        return newStream(new IntIteratorEx() {
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
            public int nextInt() {
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
            public int[] toArray() {
                final int[] a = new int[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = elements[((start + i) % len) + fromIndex];
                }

                return a;
            }
        }, false);
    }

    @Override
    public IntSummaryStatistics summarize() {
        assertNotClosed();

        try {
            final IntSummaryStatistics result = new IntSummaryStatistics();

            for (int i = fromIndex; i < toIndex; i++) {
                result.accept(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> boolean anyMatch(final Try.IntPredicate<E> predicate) throws E {
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
    public <E extends Exception> boolean allMatch(final Try.IntPredicate<E> predicate) throws E {
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
    public <E extends Exception> boolean noneMatch(final Try.IntPredicate<E> predicate) throws E {
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
    public <E extends Exception> OptionalInt findFirst(final Try.IntPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return OptionalInt.of(elements[i]);
                }
            }

            return OptionalInt.empty();
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalInt findLast(final Try.IntPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            for (int i = toIndex - 1; i >= fromIndex; i--) {
                if (predicate.test(elements[i])) {
                    return OptionalInt.of(elements[i]);
                }
            }

            return OptionalInt.empty();
        } finally {
            close();
        }
    }

    @Override
    public LongStream asLongStream() {
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

                return elements[cursor++];
            }

            @Override
            public long count() {
                return toIndex - cursor;
            }

            @Override
            public void skip(long n) {
                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public long[] toArray() {
                final long[] a = new long[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, sorted);
    }

    @Override
    public FloatStream asFloatStream() {
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

                return elements[cursor++];
            }

            @Override
            public long count() {
                return toIndex - cursor;
            }

            @Override
            public void skip(long n) {
                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public float[] toArray() {
                final float[] a = new float[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, sorted);
    }

    @Override
    public DoubleStream asDoubleStream() {
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

                return elements[cursor++];
            }

            @Override
            public long count() {
                return toIndex - cursor;
            }

            @Override
            public void skip(long n) {
                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public double[] toArray() {
                final double[] a = new double[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, sorted);
    }

    @Override
    public java.util.stream.IntStream toJdkStream() {
        java.util.stream.IntStream s = java.util.stream.IntStream.of(elements);

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
    public Stream<Integer> boxed() {
        return new IteratorStream<>(iteratorEx(), sorted, sorted ? INT_COMPARATOR : null, closeHandlers);
    }

    //    @Override
    //    public IntStream cached() {
    //        return this;
    //    }

    @Override
    IntIteratorEx iteratorEx() {
        return IntIteratorEx.of(elements, fromIndex, toIndex);
    }

    @Override
    public IntStream appendIfEmpty(final Supplier<IntStream> supplier) {
        if (fromIndex == toIndex) {
            return append(supplier.get());
        } else {
            return this;
        }
    }

    @Override
    public IntStream parallel(final int maxThreadNum, final Splitor splitor) {
        return new ParallelArrayIntStream(elements, fromIndex, toIndex, sorted, checkMaxThreadNum(maxThreadNum), checkSplitor(splitor), asyncExecutor(),
                closeHandlers);
    }

    @Override
    public IntStream parallel(final int maxThreadNum, final Executor executor) {
        return new ParallelArrayIntStream(elements, fromIndex, toIndex, sorted, checkMaxThreadNum(maxThreadNum), splitor(), createAsyncExecutor(executor),
                closeHandlers);
    }

    @Override
    public IntStream onClose(Runnable closeHandler) {
        final Deque<Runnable> newCloseHandlers = new LocalArrayDeque<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        newCloseHandlers.add(wrapCloseHandlers(closeHandler));

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        return new ArrayIntStream(elements, fromIndex, toIndex, sorted, newCloseHandlers);
    }
}

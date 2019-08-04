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
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executor;

import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.StringUtil.Strings;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharToIntFunction;
import com.landawn.abacus.util.function.CharUnaryOperator;
import com.landawn.abacus.util.function.ObjCharConsumer;
import com.landawn.abacus.util.function.Supplier;

/**
 * 
 */
class ArrayCharStream extends AbstractCharStream {
    final char[] elements;
    final int fromIndex;
    final int toIndex;

    ArrayCharStream(final char[] values) {
        this(values, 0, values.length);
    }

    ArrayCharStream(final char[] values, final Collection<Runnable> closeHandlers) {
        this(values, 0, values.length, closeHandlers);
    }

    ArrayCharStream(final char[] values, final boolean sorted, final Collection<Runnable> closeHandlers) {
        this(values, 0, values.length, sorted, closeHandlers);
    }

    ArrayCharStream(final char[] values, final int fromIndex, final int toIndex) {
        this(values, fromIndex, toIndex, null);
    }

    ArrayCharStream(final char[] values, final int fromIndex, final int toIndex, final Collection<Runnable> closeHandlers) {
        this(values, fromIndex, toIndex, false, closeHandlers);
    }

    ArrayCharStream(final char[] values, final int fromIndex, final int toIndex, final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, closeHandlers);

        checkFromToIndex(fromIndex, toIndex, N.len(values));

        this.elements = values;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    @Override
    public CharStream filter(final CharPredicate predicate) {
        return newStream(new CharIteratorEx() {
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
            public char nextChar() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted);
    }

    @Override
    public CharStream takeWhile(final CharPredicate predicate) {
        return newStream(new CharIteratorEx() {
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
            public char nextChar() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted);
    }

    @Override
    public CharStream dropWhile(final CharPredicate predicate) {
        return newStream(new CharIteratorEx() {
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
            public char nextChar() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted);
    }

    @Override
    public CharStream step(final long step) {
        checkArgPositive(step, "step");

        if (step == 1 || fromIndex == toIndex) {
            return newStream(elements, fromIndex, toIndex, sorted);
        }

        return newStream(new CharIteratorEx() {
            private final int stepp = (int) N.min(step, Integer.MAX_VALUE);
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

                final char res = elements[cursor];
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
            public char[] toArray() {
                final char[] a = new char[(int) count()];

                for (int i = 0, len = a.length; i < len; i++, cursor += stepp) {
                    a[i] = elements[cursor];
                }

                return a;
            }
        }, sorted);
    }

    @Override
    public CharStream map(final CharUnaryOperator mapper) {
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
    public IntStream mapToInt(final CharToIntFunction mapper) {
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
    public <U> Stream<U> mapToObj(final CharFunction<? extends U> mapper) {
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
    public CharStream flatMap(final CharFunction<? extends CharStream> mapper) {
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
    public IntStream flatMapToInt(final CharFunction<? extends IntStream> mapper) {
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
    public <T> Stream<T> flatMapToObj(final CharFunction<? extends Stream<T>> mapper) {
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
    public Stream<CharStream> split(final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        return newStream(new ObjIteratorEx<CharStream>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public CharStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return new ArrayCharStream(elements, cursor, (cursor = chunkSize < toIndex - cursor ? cursor + chunkSize : toIndex), sorted, null);
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
    public Stream<CharList> splitToList(final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        return newStream(new ObjIteratorEx<CharList>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public CharList next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return new CharList(N.copyOfRange(elements, cursor, (cursor = chunkSize < toIndex - cursor ? cursor + chunkSize : toIndex)));
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
    public Stream<CharStream> split(final CharPredicate predicate) {
        return newStream(new ObjIteratorEx<CharStream>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public CharStream next() {
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

                return new ArrayCharStream(elements, from, cursor, sorted, null);
            }
        }, false, null);
    }

    @Override
    public Stream<CharList> splitToList(final CharPredicate predicate) {
        return newStream(new ObjIteratorEx<CharList>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public CharList next() {
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

                return new CharList(N.copyOfRange(elements, from, cursor));
            }
        }, false, null);
    }

    @Override
    public Stream<CharStream> splitAt(final int where) {
        checkArgNotNegative(where, "where");

        final CharStream[] a = new CharStream[2];
        final int middleIndex = where < toIndex - fromIndex ? fromIndex + where : toIndex;
        a[0] = middleIndex == fromIndex ? CharStream.empty() : new ArrayCharStream(elements, fromIndex, middleIndex, sorted, null);
        a[1] = middleIndex == toIndex ? CharStream.empty() : new ArrayCharStream(elements, middleIndex, toIndex, sorted, null);

        return newStream(a, false, null);
    }

    @Override
    public Stream<CharStream> sliding(final int windowSize, final int increment) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        return newStream(new ObjIteratorEx<CharStream>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public CharStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final ArrayCharStream result = new ArrayCharStream(elements, cursor, windowSize < toIndex - cursor ? cursor + windowSize : toIndex, sorted,
                        null);

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
    public Stream<CharList> slidingToList(final int windowSize, final int increment) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        return newStream(new ObjIteratorEx<CharList>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public CharList next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final CharList result = CharList.of(N.copyOfRange(elements, cursor, windowSize < toIndex - cursor ? cursor + windowSize : toIndex));

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
    public CharStream peek(final CharConsumer action) {
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

                action.accept(elements[cursor]);

                return elements[cursor++];
            }

            @Override
            public char[] toArray() {
                final char[] a = new char[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    action.accept(elements[cursor]);

                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, sorted);
    }

    @Override
    public CharStream limit(final long maxSize) {
        checkArgNotNegative(maxSize, "maxSize");

        return newStream(elements, fromIndex, (int) (fromIndex + maxSize), sorted);
    }

    @Override
    public CharStream skip(long n) {
        checkArgNotNegative(n, "n");

        if (n >= toIndex - fromIndex) {
            return newStream(elements, toIndex, toIndex, sorted);
        } else {
            return newStream(elements, (int) (fromIndex + n), toIndex, sorted);
        }
    }

    @Override
    public <E extends Exception> void forEach(final Try.CharConsumer<E> action) throws E {
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
    public char[] toArray() {
        assertNotClosed();

        try {
            return N.copyOfRange(elements, fromIndex, toIndex);
        } finally {
            close();
        }
    }

    @Override
    public CharList toCharList() {
        assertNotClosed();

        try {
            return CharList.of(N.copyOfRange(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    @Override
    public List<Character> toList() {
        assertNotClosed();

        try {
            final List<Character> result = new ArrayList<>(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Set<Character> toSet() {
        assertNotClosed();

        try {
            final Set<Character> result = new HashSet<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <C extends Collection<Character>> C toCollection(Supplier<? extends C> supplier) {
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
    public Multiset<Character> toMultiset() {
        assertNotClosed();

        try {
            final Multiset<Character> result = new Multiset<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<Character> toMultiset(Supplier<? extends Multiset<Character>> supplier) {
        assertNotClosed();

        try {
            final Multiset<Character> result = supplier.get();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public LongMultiset<Character> toLongMultiset() {
        assertNotClosed();

        try {
            final LongMultiset<Character> result = new LongMultiset<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public LongMultiset<Character> toLongMultiset(Supplier<? extends LongMultiset<Character>> supplier) {
        assertNotClosed();

        try {
            final LongMultiset<Character> result = supplier.get();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(CharFunction<? extends K> keyMapper, CharFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction,
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
    public <K, A, D, M extends Map<K, D>> M toMap(final CharFunction<? extends K> keyMapper, final Collector<Character, A, D> downstream,
            final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            final Supplier<A> downstreamSupplier = downstream.supplier();
            final BiConsumer<A, Character> downstreamAccumulator = downstream.accumulator();
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
    public OptionalChar first() {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalChar.of(elements[fromIndex]) : OptionalChar.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalChar last() {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalChar.of(elements[toIndex - 1]) : OptionalChar.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalChar onlyOne() throws DuplicatedResultException {
        assertNotClosed();

        try {
            final int size = toIndex - fromIndex;

            if (size == 0) {
                return OptionalChar.empty();
            } else if (size == 1) {
                return OptionalChar.of(elements[fromIndex]);
            } else {
                throw new DuplicatedResultException("There are at least two elements: " + Strings.concat(elements[fromIndex], ", ", elements[fromIndex + 1]));
            }
        } finally {
            close();
        }
    }

    @Override
    public char reduce(char identity, CharBinaryOperator op) {
        assertNotClosed();

        try {
            char result = identity;

            for (int i = fromIndex; i < toIndex; i++) {
                result = op.applyAsChar(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalChar reduce(CharBinaryOperator op) {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalChar.empty();
            }

            char result = elements[fromIndex];

            for (int i = fromIndex + 1; i < toIndex; i++) {
                result = op.applyAsChar(result, elements[i]);
            }

            return OptionalChar.of(result);
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjCharConsumer<? super R> accumulator, BiConsumer<R, R> combiner) {
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
    public OptionalChar min() {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalChar.empty();
            } else if (sorted) {
                return OptionalChar.of(elements[fromIndex]);
            }

            return OptionalChar.of(N.min(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    @Override
    public OptionalChar max() {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalChar.empty();
            } else if (sorted) {
                return OptionalChar.of(elements[toIndex - 1]);
            }

            return OptionalChar.of(N.max(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    @Override
    public OptionalChar kthLargest(int k) {
        checkArgPositive(k, "k");
        assertNotClosed();

        try {
            if (k > toIndex - fromIndex) {
                return OptionalChar.empty();
            } else if (sorted) {
                return OptionalChar.of(elements[toIndex - k]);
            }

            return OptionalChar.of(N.kthLargest(elements, fromIndex, toIndex, k));
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
    public CharStream reversed() {
        return newStream(new CharIteratorEx() {
            private int cursor = toIndex;

            @Override
            public boolean hasNext() {
                return cursor > fromIndex;
            }

            @Override
            public char nextChar() {
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
            public char[] toArray() {
                final char[] a = new char[cursor - fromIndex];

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = elements[cursor - i - 1];
                }

                return a;
            }
        }, false);
    }

    @Override
    public CharStream rotated(final int distance) {
        if (distance == 0 || toIndex - fromIndex <= 1 || distance % (toIndex - fromIndex) == 0) {
            return newStream(elements, fromIndex, toIndex, sorted);
        }

        return newStream(new CharIteratorEx() {
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
            public char nextChar() {
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
            public char[] toArray() {
                final char[] a = new char[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = elements[((start + i) % len) + fromIndex];
                }

                return a;
            }
        }, false);
    }

    @Override
    public CharSummaryStatistics summarize() {
        assertNotClosed();

        try {
            final CharSummaryStatistics result = new CharSummaryStatistics();

            for (int i = fromIndex; i < toIndex; i++) {
                result.accept(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> boolean anyMatch(final Try.CharPredicate<E> predicate) throws E {
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
    public <E extends Exception> boolean allMatch(final Try.CharPredicate<E> predicate) throws E {
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
    public <E extends Exception> boolean noneMatch(final Try.CharPredicate<E> predicate) throws E {
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
    public <E extends Exception> OptionalChar findFirst(final Try.CharPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return OptionalChar.of(elements[i]);
                }
            }

            return OptionalChar.empty();
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalChar findLast(final Try.CharPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            for (int i = toIndex - 1; i >= fromIndex; i--) {
                if (predicate.test(elements[i])) {
                    return OptionalChar.of(elements[i]);
                }
            }

            return OptionalChar.empty();
        } finally {
            close();
        }
    }

    @Override
    public IntStream asIntStream() {
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
            public int[] toArray() {
                final int[] a = new int[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, sorted);
    }

    @Override
    public Stream<Character> boxed() {
        return new IteratorStream<>(iteratorEx(), sorted, sorted ? CHAR_COMPARATOR : null, closeHandlers);
    }

    //    @Override
    //    public CharStream cached() {
    //        return this;
    //    }

    @Override
    CharIteratorEx iteratorEx() {
        return CharIteratorEx.of(elements, fromIndex, toIndex);
    }

    @Override
    public CharStream appendIfEmpty(final Supplier<CharStream> supplier) {
        if (fromIndex == toIndex) {
            return append(supplier.get());
        } else {
            return this;
        }
    }

    @Override
    public CharStream parallel(final int maxThreadNum, final Splitor splitor) {
        return new ParallelArrayCharStream(elements, fromIndex, toIndex, sorted, checkMaxThreadNum(maxThreadNum), checkSplitor(splitor), asyncExecutor(),
                closeHandlers);
    }

    @Override
    public CharStream parallel(final int maxThreadNum, final Executor executor) {
        return new ParallelArrayCharStream(elements, fromIndex, toIndex, sorted, checkMaxThreadNum(maxThreadNum), splitor(), createAsyncExecutor(executor),
                closeHandlers);
    }

    @Override
    public CharStream onClose(Runnable closeHandler) {
        final Deque<Runnable> newCloseHandlers = new LocalArrayDeque<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        newCloseHandlers.add(wrapCloseHandlers(closeHandler));

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        return new ArrayCharStream(elements, fromIndex, toIndex, sorted, newCloseHandlers);
    }
}

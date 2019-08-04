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

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executor;

import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteToIntFunction;
import com.landawn.abacus.util.function.ByteUnaryOperator;
import com.landawn.abacus.util.function.ObjByteConsumer;
import com.landawn.abacus.util.function.Supplier;

/**
 * 
 */
class IteratorByteStream extends AbstractByteStream {
    ByteIteratorEx elements;

    //    OptionalByte head;
    //    ByteStream tail;

    //    ByteStream head2;
    //    OptionalByte tail2;

    IteratorByteStream(final ByteIterator values) {
        this(values, null);
    }

    IteratorByteStream(final ByteIterator values, final Collection<Runnable> closeHandlers) {
        this(values, false, closeHandlers);
    }

    IteratorByteStream(final ByteIterator values, final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, closeHandlers);

        ByteIteratorEx tmp = null;

        if (values instanceof ByteIteratorEx) {
            tmp = (ByteIteratorEx) values;
        } else {
            tmp = new ByteIteratorEx() {
                @Override
                public boolean hasNext() {
                    return values.hasNext();
                }

                @Override
                public byte nextByte() {
                    return values.nextByte();
                }
            };
        }

        this.elements = tmp;
    }

    @Override
    public ByteStream filter(final BytePredicate predicate) {
        return newStream(new ByteIteratorEx() {
            private boolean hasNext = false;
            private byte next = 0;

            @Override
            public boolean hasNext() {
                if (hasNext == false) {
                    while (elements.hasNext()) {
                        next = elements.nextByte();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public byte nextByte() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }
        }, sorted);
    }

    @Override
    public ByteStream takeWhile(final BytePredicate predicate) {
        return newStream(new ByteIteratorEx() {
            private boolean hasMore = true;
            private boolean hasNext = false;
            private byte next = 0;

            @Override
            public boolean hasNext() {
                if (hasNext == false && hasMore && elements.hasNext()) {
                    next = elements.nextByte();

                    if (predicate.test(next)) {
                        hasNext = true;
                    } else {
                        hasMore = false;
                    }
                }

                return hasNext;
            }

            @Override
            public byte nextByte() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }

        }, sorted);
    }

    @Override
    public ByteStream dropWhile(final BytePredicate predicate) {
        return newStream(new ByteIteratorEx() {
            private boolean hasNext = false;
            private byte next = 0;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (hasNext == false) {
                    if (dropped == false) {
                        dropped = true;

                        while (elements.hasNext()) {
                            next = elements.nextByte();

                            if (predicate.test(next) == false) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else if (elements.hasNext()) {
                        next = elements.nextByte();
                        hasNext = true;
                    }
                }

                return hasNext;
            }

            @Override
            public byte nextByte() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }

        }, sorted);
    }

    @Override
    public ByteStream map(final ByteUnaryOperator mapper) {
        return newStream(new ByteIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public byte nextByte() {
                return mapper.applyAsByte(elements.nextByte());
            }

            //    @Override
            //    public long count() {
            //        return elements.count();
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        elements.skip(n);
            //    }
        }, false);
    }

    @Override
    public IntStream mapToInt(final ByteToIntFunction mapper) {
        return newStream(new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public int nextInt() {
                return mapper.applyAsInt(elements.nextByte());
            }

            //    @Override
            //    public long count() {
            //        return elements.count();
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        elements.skip(n);
            //    }
        }, false);
    }

    @Override
    public <U> Stream<U> mapToObj(final ByteFunction<? extends U> mapper) {
        return newStream(new ObjIteratorEx<U>() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public U next() {
                return mapper.apply(elements.nextByte());
            }

            //    @Override
            //    public long count() {
            //        return elements.count();
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        elements.skip(n);
            //    }
        }, false, null);
    }

    @Override
    public ByteStream flatMap(final ByteFunction<? extends ByteStream> mapper) {
        final ByteIteratorEx iter = new ByteIteratorEx() {
            private ByteIterator cur = null;
            private ByteStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements.nextByte());

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
    public IntStream flatMapToInt(final ByteFunction<? extends IntStream> mapper) {
        final IntIteratorEx iter = new IntIteratorEx() {
            private IntIterator cur = null;
            private IntStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements.nextByte());

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
    public <T> Stream<T> flatMapToObj(final ByteFunction<? extends Stream<T>> mapper) {
        final ObjIteratorEx<T> iter = new ObjIteratorEx<T>() {
            private Iterator<? extends T> cur = null;
            private Stream<? extends T> s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements.nextByte());

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
    public Stream<ByteList> splitToList(final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        return newStream(new ObjIteratorEx<ByteList>() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public ByteList next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final ByteList result = new ByteList(chunkSize);

                while (result.size() < chunkSize && elements.hasNext()) {
                    result.add(elements.nextByte());
                }

                return result;
            }

            @Override
            public long count() {
                final long len = elements.count();
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void skip(long n) {
                elements.skip(n > Long.MAX_VALUE / chunkSize ? Long.MAX_VALUE : n * chunkSize);
            }
        }, false, null);
    }

    @Override
    public Stream<ByteList> splitToList(final BytePredicate predicate) {
        return newStream(new ObjIteratorEx<ByteList>() {
            private byte next;
            private boolean hasNext = false;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return hasNext == true || elements.hasNext();
            }

            @Override
            public ByteList next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final ByteList result = new ByteList();

                if (hasNext == false) {
                    next = elements.nextByte();
                    hasNext = true;
                }

                while (hasNext) {
                    if (result.size() == 0) {
                        result.add(next);
                        preCondition = predicate.test(next);
                        next = (hasNext = elements.hasNext()) ? elements.nextByte() : 0;
                    } else if (predicate.test(next) == preCondition) {
                        result.add(next);
                        next = (hasNext = elements.hasNext()) ? elements.nextByte() : 0;
                    } else {
                        break;
                    }
                }

                return result;
            }

        }, false, null);
    }

    @Override
    public Stream<ByteStream> splitAt(final int where) {
        checkArgNotNegative(where, "where");

        return newStream(new ObjIteratorEx<ByteStream>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public ByteStream next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                ByteStream result = null;

                if (cursor == 0) {
                    final ByteList list = new ByteList();
                    int cnt = 0;

                    while (cnt++ < where && elements.hasNext()) {
                        list.add(elements.nextByte());
                    }

                    result = new ArrayByteStream(list.array(), 0, list.size(), sorted, null);
                } else {
                    result = new IteratorByteStream(elements, sorted, null);
                }

                cursor++;

                return result;
            }

            @Override
            public long count() {
                elements.count();

                return 2 - cursor;
            }

            @Override
            public void skip(long n) {
                if (n == 0) {
                    return;
                } else if (n == 1) {
                    if (cursor == 0) {
                        elements.skip(where);
                    } else {
                        elements.skip(Long.MAX_VALUE);
                    }
                } else {
                    elements.skip(Long.MAX_VALUE);
                }

                cursor = n >= 2 ? 2 : cursor + (int) n;
            }
        }, false, null);
    }

    @Override
    public Stream<ByteList> slidingToList(final int windowSize, final int increment) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        return newStream(new ObjIteratorEx<ByteList>() {
            private ByteList prev = null;
            private boolean toSkip = false;

            @Override
            public boolean hasNext() {
                if (toSkip) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.nextByte();
                    }

                    toSkip = false;
                }

                return elements.hasNext();
            }

            @Override
            public ByteList next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                ByteList result = null;
                int cnt = 0;

                if (prev != null && increment < windowSize) {
                    cnt = windowSize - increment;

                    if (cnt <= 8) {
                        result = new ByteList(windowSize);

                        for (int i = windowSize - cnt; i < windowSize; i++) {
                            result.add(prev.get(i));
                        }
                    } else {
                        final byte[] dest = new byte[windowSize];
                        N.copy(prev.array(), windowSize - cnt, dest, 0, cnt);
                        result = ByteList.of(dest, cnt);
                    }
                }

                if (result == null) {
                    result = new ByteList(windowSize);
                }

                while (cnt++ < windowSize && elements.hasNext()) {
                    result.add(elements.nextByte());
                }

                toSkip = increment > windowSize;

                return prev = result;
            }

            @Override
            public long count() {
                final int prevSize = increment >= windowSize ? 0 : (prev == null ? 0 : prev.size());
                final long len = prevSize + elements.count();

                if (len == prevSize) {
                    return 0;
                } else if (len <= windowSize) {
                    return 1;
                } else {
                    final long rlen = len - windowSize;
                    return 1 + (rlen % increment == 0 ? rlen / increment : rlen / increment + 1);
                }
            }

            @Override
            public void skip(long n) {
                if (n == 0) {
                    return;
                }

                if (increment >= windowSize) {
                    elements.skip(n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                } else {
                    final ByteList tmp = new ByteList(windowSize);

                    if (N.isNullOrEmpty(prev)) {
                        final long m = ((n - 1) > Long.MAX_VALUE / increment ? Long.MAX_VALUE : (n - 1) * increment);
                        elements.skip(m);
                    } else {
                        final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                        final int prevSize = increment >= windowSize ? 0 : (prev == null ? 0 : prev.size());

                        if (m < prevSize) {
                            tmp.addAll(prev.copy((int) m, prevSize));
                        } else {
                            elements.skip(m - prevSize);
                        }
                    }

                    int cnt = tmp.size();

                    while (cnt++ < windowSize && elements.hasNext()) {
                        tmp.add(elements.nextByte());
                    }

                    prev = tmp;
                }
            }
        }, false, null);
    }

    @Override
    public ByteStream peek(final ByteConsumer action) {
        return newStream(new ByteIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public byte nextByte() {
                final byte next = elements.nextByte();

                action.accept(next);
                return next;
            }
        }, sorted);
    }

    @Override
    public ByteStream limit(final long maxSize) {
        checkArgNotNegative(maxSize, "maxSize");

        return newStream(new ByteIteratorEx() {
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public byte nextByte() {
                if (cnt >= maxSize) {
                    throw new NoSuchElementException();
                }

                cnt++;
                return elements.nextByte();
            }

            @Override
            public void skip(long n) {
                elements.skip(n);
            }
        }, sorted);
    }

    @Override
    public ByteStream skip(final long n) {
        checkArgNotNegative(n, "n");

        return newStream(new ByteIteratorEx() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                return elements.hasNext();
            }

            @Override
            public byte nextByte() {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                return elements.nextByte();
            }

            @Override
            public long count() {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                return elements.count();
            }

            @Override
            public void skip(long n2) {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                elements.skip(n2);
            }

            @Override
            public byte[] toArray() {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                return elements.toArray();
            }
        }, sorted);
    }

    @Override
    public <E extends Exception> void forEach(final Try.ByteConsumer<E> action) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                action.accept(elements.nextByte());
            }
        } finally {
            close();
        }
    }

    @Override
    public byte[] toArray() {
        assertNotClosed();

        try {
            return elements.toArray();
        } finally {
            close();
        }
    }

    @Override
    public ByteList toByteList() {
        assertNotClosed();

        try {
            return elements.toList();
        } finally {
            close();
        }
    }

    @Override
    public List<Byte> toList() {
        return toCollection(Suppliers.<Byte> ofList());
    }

    @Override
    public Set<Byte> toSet() {
        return toCollection(Suppliers.<Byte> ofSet());
    }

    @Override
    public <C extends Collection<Byte>> C toCollection(Supplier<? extends C> supplier) {
        assertNotClosed();

        try {
            final C result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextByte());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<Byte> toMultiset() {
        return toMultiset(Suppliers.<Byte> ofMultiset());
    }

    @Override
    public Multiset<Byte> toMultiset(Supplier<? extends Multiset<Byte>> supplier) {
        assertNotClosed();

        try {
            final Multiset<Byte> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextByte());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public LongMultiset<Byte> toLongMultiset() {
        return toLongMultiset(Suppliers.<Byte> ofLongMultiset());
    }

    @Override
    public LongMultiset<Byte> toLongMultiset(Supplier<? extends LongMultiset<Byte>> supplier) {
        assertNotClosed();

        try {
            final LongMultiset<Byte> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextByte());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(ByteFunction<? extends K> keyMapper, ByteFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction,
            Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            byte next = 0;

            while (elements.hasNext()) {
                next = elements.nextByte();
                Collectors.merge(result, keyMapper.apply(next), valueMapper.apply(next), mergeFunction);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, A, D, M extends Map<K, D>> M toMap(final ByteFunction<? extends K> keyMapper, final Collector<Byte, A, D> downstream,
            final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            final Supplier<A> downstreamSupplier = downstream.supplier();
            final BiConsumer<A, Byte> downstreamAccumulator = downstream.accumulator();
            final Map<K, A> intermediate = (Map<K, A>) result;
            K key = null;
            A v = null;
            byte next = 0;

            while (elements.hasNext()) {
                next = elements.nextByte();
                key = checkArgNotNull(keyMapper.apply(next), "element cannot be mapped to a null key");

                if ((v = intermediate.get(key)) == null) {
                    if ((v = downstreamSupplier.get()) != null) {
                        intermediate.put(key, v);
                    }
                }

                downstreamAccumulator.accept(v, next);
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
    public byte reduce(byte identity, ByteBinaryOperator op) {
        assertNotClosed();

        try {
            byte result = identity;

            while (elements.hasNext()) {
                result = op.applyAsByte(result, elements.nextByte());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalByte reduce(ByteBinaryOperator op) {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalByte.empty();
            }

            byte result = elements.nextByte();

            while (elements.hasNext()) {
                result = op.applyAsByte(result, elements.nextByte());
            }

            return OptionalByte.of(result);
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjByteConsumer<? super R> accumulator, BiConsumer<R, R> combiner) {
        assertNotClosed();

        try {
            final R result = supplier.get();

            while (elements.hasNext()) {
                accumulator.accept(result, elements.nextByte());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalByte min() {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalByte.empty();
            } else if (sorted) {
                return OptionalByte.of(elements.nextByte());
            }

            byte candidate = elements.nextByte();
            byte next = 0;

            while (elements.hasNext()) {
                next = elements.nextByte();

                if (next < candidate) {
                    candidate = next;
                }
            }

            return OptionalByte.of(candidate);
        } finally {
            close();
        }
    }

    @Override
    public OptionalByte max() {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalByte.empty();
            } else if (sorted) {
                byte next = 0;

                while (elements.hasNext()) {
                    next = elements.nextByte();
                }

                return OptionalByte.of(next);
            }

            byte candidate = elements.nextByte();
            byte next = 0;

            while (elements.hasNext()) {
                next = elements.nextByte();

                if (next > candidate) {
                    candidate = next;
                }
            }

            return OptionalByte.of(candidate);
        } finally {
            close();
        }
    }

    @Override
    public OptionalByte kthLargest(int k) {
        checkArgPositive(k, "k");
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalByte.empty();
            }

            final Optional<Byte> optional = boxed().kthLargest(k, BYTE_COMPARATOR);

            return optional.isPresent() ? OptionalByte.of(optional.get()) : OptionalByte.empty();
        } finally {
            close();
        }
    }

    @Override
    public int sum() {
        assertNotClosed();

        try {
            long result = 0;

            while (elements.hasNext()) {
                result += elements.nextByte();
            }

            return N.toIntExact(result);
        } finally {
            close();
        }
    }

    @Override
    public OptionalDouble average() {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalDouble.empty();
            }

            long sum = 0;
            long count = 0;

            while (elements.hasNext()) {
                sum += elements.nextByte();
                count++;
            }

            return OptionalDouble.of(((double) sum) / count);
        } finally {
            close();
        }
    }

    @Override
    public long count() {
        assertNotClosed();

        try {
            return elements.count();
        } finally {
            close();
        }
    }

    @Override
    public ByteSummaryStatistics summarize() {
        assertNotClosed();

        try {
            final ByteSummaryStatistics result = new ByteSummaryStatistics();

            while (elements.hasNext()) {
                result.accept(elements.nextByte());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> boolean anyMatch(final Try.BytePredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextByte())) {
                    return true;
                }
            }
        } finally {
            close();
        }

        return false;
    }

    @Override
    public <E extends Exception> boolean allMatch(final Try.BytePredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextByte()) == false) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> boolean noneMatch(final Try.BytePredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextByte())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> OptionalByte findFirst(final Try.BytePredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                byte e = elements.nextByte();

                if (predicate.test(e)) {
                    return OptionalByte.of(e);
                }
            }
        } finally {
            close();
        }

        return OptionalByte.empty();
    }

    @Override
    public <E extends Exception> OptionalByte findLast(final Try.BytePredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalByte.empty();
            }

            boolean hasResult = false;
            byte e = 0;
            byte result = 0;

            while (elements.hasNext()) {
                e = elements.nextByte();

                if (predicate.test(e)) {
                    result = e;
                    hasResult = true;
                }
            }

            return hasResult ? OptionalByte.of(result) : OptionalByte.empty();
        } finally {
            close();
        }
    }

    @Override
    public IntStream asIntStream() {
        return newStream(new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public int nextInt() {
                return elements.nextByte();
            }

            @Override
            public long count() {
                return elements.count();
            }

            @Override
            public void skip(long n) {
                elements.skip(n);
            }
        }, sorted);
    }

    @Override
    public Stream<Byte> boxed() {
        return new IteratorStream<>(iteratorEx(), sorted, sorted ? BYTE_COMPARATOR : null, closeHandlers);
    }

    @Override
    ByteIteratorEx iteratorEx() {
        return elements;
    }

    @Override
    public ByteStream appendIfEmpty(final Supplier<ByteStream> supplier) {
        if (elements.hasNext() == false) {
            return append(supplier.get());
        } else {
            return this;
        }
    }

    @Override
    public ByteStream parallel(int maxThreadNum, Splitor splitor) {
        return new ParallelIteratorByteStream(elements, sorted, checkMaxThreadNum(maxThreadNum), checkSplitor(splitor), asyncExecutor(), closeHandlers);
    }

    @Override
    public ByteStream parallel(final int maxThreadNum, final Executor executor) {
        return new ParallelIteratorByteStream(elements, sorted, checkMaxThreadNum(maxThreadNum), splitor(), createAsyncExecutor(executor), closeHandlers);
    }

    @Override
    public ByteStream onClose(Runnable closeHandler) {
        final Deque<Runnable> newCloseHandlers = new LocalArrayDeque<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        newCloseHandlers.add(wrapCloseHandlers(closeHandler));

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        return new IteratorByteStream(elements, sorted, newCloseHandlers);
    }
}

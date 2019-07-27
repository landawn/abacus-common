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
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.IndexedByte;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableByte;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.StringUtil.Strings;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteBiPredicate;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteTernaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ObjByteConsumer;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;

/**
 * 
 */
abstract class AbstractByteStream extends ByteStream {

    AbstractByteStream(final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, closeHandlers);
    }

    @Override
    public ByteStream distinct() {
        final Set<Object> set = new HashSet<>();

        return newStream(this.sequential().filter(new BytePredicate() {
            @Override
            public boolean test(byte value) {
                return set.add(value);
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public ByteStream flattMap(final ByteFunction<byte[]> mapper) {
        return flatMap(new ByteFunction<ByteStream>() {
            @Override
            public ByteStream apply(byte t) {
                return ByteStream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public <T> Stream<T> flattMapToObj(final ByteFunction<? extends Collection<T>> mapper) {
        return flatMapToObj(new ByteFunction<Stream<T>>() {
            @Override
            public Stream<T> apply(byte t) {
                return Stream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public <T> Stream<T> flatMappToObj(final ByteFunction<T[]> mapper) {
        return flatMapToObj(new ByteFunction<Stream<T>>() {
            @Override
            public Stream<T> apply(byte t) {
                return Stream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public ByteStream rangeMap(final ByteBiPredicate sameRange, final ByteBinaryOperator mapper) {
        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ByteIteratorEx() {
            private byte left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public byte nextByte() {
                left = hasNext ? next : iter.nextByte();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextByte();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.applyAsByte(left, right);
            }
        }, false);
    }

    @Override
    public <T> Stream<T> rangeMapp(final ByteBiPredicate sameRange, final ByteBiFunction<T> mapper) {
        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<T>() {
            private byte left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                left = hasNext ? next : iter.nextByte();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextByte();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.apply(left, right);
            }
        }, false, null);
    }

    @Override
    public Stream<ByteList> collapse(final ByteBiPredicate collapsible) {
        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<ByteList>() {
            private boolean hasNext = false;
            private byte next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public ByteList next() {
                final ByteList result = new ByteList(9);
                result.add(hasNext ? next : (next = iter.nextByte()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextByte()))) {
                        result.add(next);
                    } else {
                        break;
                    }
                }

                return result;
            }
        }, false, null);
    }

    @Override
    public ByteStream collapse(final ByteBiPredicate collapsible, final ByteBinaryOperator mergeFunction) {
        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ByteIteratorEx() {
            private boolean hasNext = false;
            private byte next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public byte nextByte() {
                byte res = hasNext ? next : (next = iter.nextByte());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextByte()))) {
                        res = mergeFunction.applyAsByte(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public ByteStream skip(final long n, final ByteConsumer action) {
        final BytePredicate filter = isParallel() ? new BytePredicate() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(byte value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new BytePredicate() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(byte value) {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public ByteStream removeIf(final BytePredicate predicate) {
        checkArgNotNull(predicate);

        return filter(new BytePredicate() {
            @Override
            public boolean test(byte value) {
                return predicate.test(value) == false;
            }
        });
    }

    @Override
    public ByteStream removeIf(final BytePredicate predicate, final ByteConsumer action) {
        checkArgNotNull(predicate);
        checkArgNotNull(predicate);

        return filter(new BytePredicate() {
            @Override
            public boolean test(byte value) {
                if (predicate.test(value)) {
                    action.accept(value);
                    return false;
                }

                return true;
            }
        });
    }

    @Override
    public ByteStream dropWhile(final BytePredicate predicate, final ByteConsumer action) {
        checkArgNotNull(predicate);
        checkArgNotNull(action);

        return dropWhile(new BytePredicate() {
            @Override
            public boolean test(byte value) {
                if (predicate.test(value)) {
                    action.accept(value);
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public ByteStream step(final long step) {
        checkArgPositive(step, "step");

        final long skip = step - 1;
        final ByteIteratorEx iter = this.iteratorEx();

        final ByteIterator byteIterator = new ByteIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                final byte next = iter.nextByte();
                iter.skip(skip);
                return next;
            }
        };

        return newStream(byteIterator, sorted);
    }

    @Override
    public Stream<ByteStream> split(final int chunkSize) {
        return splitToList(chunkSize).map(new Function<ByteList, ByteStream>() {
            @Override
            public ByteStream apply(ByteList t) {
                return new ArrayByteStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public Stream<ByteStream> split(final BytePredicate predicate) {
        return splitToList(predicate).map(new Function<ByteList, ByteStream>() {
            @Override
            public ByteStream apply(ByteList t) {
                return new ArrayByteStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public Stream<ByteStream> splitBy(final BytePredicate where) {
        checkArgNotNull(where);

        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<ByteStream>() {
            private int cursor = 0;
            private byte next = 0;
            private boolean hasNext = false;

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

                    while (iter.hasNext()) {
                        next = iter.nextByte();

                        if (where.test(next)) {
                            list.add(next);
                        } else {
                            hasNext = true;
                            break;
                        }
                    }

                    result = new ArrayByteStream(list.array(), 0, list.size(), sorted, null);
                } else {
                    ByteIteratorEx iterEx = iter;

                    if (hasNext) {
                        iterEx = new ByteIteratorEx() {
                            private boolean isFirst = true;

                            @Override
                            public boolean hasNext() {
                                return isFirst || iter.hasNext();
                            }

                            @Override
                            public byte nextByte() {
                                if (hasNext() == false) {
                                    throw new NoSuchElementException();
                                }

                                if (isFirst) {
                                    isFirst = false;
                                    return next;
                                } else {
                                    return iter.nextByte();
                                }
                            }
                        };
                    }

                    result = new IteratorByteStream(iterEx, sorted, null);
                }

                cursor++;

                return result;
            }

            @Override
            public long count() {
                iter.count();

                return 2 - cursor;
            }

            @Override
            public void skip(long n) {
                if (n == 0) {
                    return;
                } else if (n == 1) {
                    if (cursor == 0) {
                        while (iter.hasNext()) {
                            next = iter.nextByte();

                            if (where.test(next) == false) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else {
                        iter.skip(Long.MAX_VALUE);
                    }
                } else {
                    iter.skip(Long.MAX_VALUE);
                }

                cursor = n >= 2 ? 2 : cursor + (int) n;
            }

        }, false, null);
    }

    @Override
    public Stream<ByteStream> sliding(final int windowSize, final int increment) {
        return slidingToList(windowSize, increment).map(new Function<ByteList, ByteStream>() {
            @Override
            public ByteStream apply(ByteList t) {
                return new ArrayByteStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public ByteStream scan(final ByteBinaryOperator accumulator) {
        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ByteIteratorEx() {
            private byte res = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                if (isFirst) {
                    isFirst = false;
                    return (res = iter.nextByte());
                } else {
                    return (res = accumulator.applyAsByte(res, iter.nextByte()));
                }
            }
        }, false);
    }

    @Override
    public ByteStream scan(final byte init, final ByteBinaryOperator accumulator) {
        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ByteIteratorEx() {
            private byte res = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                return (res = accumulator.applyAsByte(res, iter.nextByte()));
            }
        }, false);
    }

    @Override
    public ByteStream scan(final byte init, final ByteBinaryOperator accumulator, final boolean initIncluded) {
        if (initIncluded == false) {
            return scan(init, accumulator);
        }

        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ByteIteratorEx() {
            private boolean isFirst = true;
            private byte res = init;

            @Override
            public boolean hasNext() {
                return isFirst || iter.hasNext();
            }

            @Override
            public byte nextByte() {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (res = accumulator.applyAsByte(res, iter.nextByte()));
            }
        }, false);
    }

    @Override
    public ByteStream intersection(final Collection<?> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new BytePredicate() {
            @Override
            public boolean test(byte value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public ByteStream difference(final Collection<?> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new BytePredicate() {
            @Override
            public boolean test(byte value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public ByteStream symmetricDifference(final Collection<Byte> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new BytePredicate() {
            @Override
            public boolean test(byte value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).append(Stream.of(c).filter(new Predicate<Byte>() {
            @Override
            public boolean test(Byte value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).mapToByte(ToByteFunction.UNBOX)).iteratorEx(), false);
    }

    @Override
    public ByteStream reversed() {
        return newStream(new ByteIteratorEx() {
            private boolean initialized = false;
            private byte[] aar;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public byte nextByte() {
                if (initialized == false) {
                    init();
                }

                if (cursor <= 0) {
                    throw new NoSuchElementException();
                }

                return aar[--cursor];
            }

            @Override
            public long count() {
                if (initialized == false) {
                    init();
                }

                return cursor;
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cursor = n < cursor ? cursor - (int) n : 0;
            }

            @Override
            public byte[] toArray() {
                if (initialized == false) {
                    init();
                }

                final byte[] a = new byte[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = aar[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = AbstractByteStream.this.toArray();
                    cursor = aar.length;
                }
            }
        }, false);
    }

    @Override
    public ByteStream shuffled(final Random rnd) {
        return lazyLoad(new Function<byte[], byte[]>() {
            @Override
            public byte[] apply(final byte[] a) {
                N.shuffle(a, rnd);
                return a;
            }
        }, false);
    }

    @Override
    public ByteStream rotated(final int distance) {
        return newStream(new ByteIteratorEx() {
            private boolean initialized = false;
            private byte[] aar;
            private int len;
            private int start;
            private int cnt = 0;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cnt < len;
            }

            @Override
            public byte nextByte() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return aar[(start + cnt++) % len];
            }

            @Override
            public long count() {
                if (initialized == false) {
                    init();
                }

                return len - cnt;
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cnt = n < len - cnt ? cnt + (int) n : len;
            }

            @Override
            public byte[] toArray() {
                if (initialized == false) {
                    init();
                }

                final byte[] a = new byte[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = aar[(start + i) % len];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = AbstractByteStream.this.toArray();
                    len = aar.length;

                    if (len > 0) {
                        start = distance % len;

                        if (start < 0) {
                            start += len;
                        }

                        start = len - start;
                    }
                }
            }
        }, distance == 0 && sorted);
    }

    @Override
    public ByteStream sorted() {
        if (sorted) {
            return newStream(iteratorEx(), sorted);
        }

        return lazyLoad(new Function<byte[], byte[]>() {
            @Override
            public byte[] apply(final byte[] a) {
                if (isParallel()) {
                    N.parallelSort(a);
                } else {
                    N.sort(a);
                }

                return a;
            }
        }, true);
    }

    @Override
    public ByteStream reverseSorted() {
        return newStream(new ByteIteratorEx() {
            private boolean initialized = false;
            private byte[] aar;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public byte nextByte() {
                if (initialized == false) {
                    init();
                }

                if (cursor <= 0) {
                    throw new NoSuchElementException();
                }

                return aar[--cursor];
            }

            @Override
            public long count() {
                if (initialized == false) {
                    init();
                }

                return cursor;
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cursor = n < cursor ? cursor - (int) n : 0;
            }

            @Override
            public byte[] toArray() {
                if (initialized == false) {
                    init();
                }

                final byte[] a = new byte[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = aar[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = AbstractByteStream.this.toArray();

                    if (isParallel()) {
                        N.parallelSort(aar);
                    } else {
                        N.sort(aar);
                    }

                    cursor = aar.length;
                }
            }
        }, false);
    }

    private ByteStream lazyLoad(final Function<byte[], byte[]> op, final boolean sorted) {
        return newStream(new ByteIteratorEx() {
            private boolean initialized = false;
            private byte[] aar;
            private int cursor = 0;
            private int len;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor < len;
            }

            @Override
            public byte nextByte() {
                if (initialized == false) {
                    init();
                }

                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return aar[cursor++];
            }

            @Override
            public long count() {
                if (initialized == false) {
                    init();
                }

                return len - cursor;
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cursor = n > len - cursor ? len : cursor + (int) n;
            }

            @Override
            public byte[] toArray() {
                if (initialized == false) {
                    init();
                }

                final byte[] a = new byte[len - cursor];

                for (int i = cursor; i < len; i++) {
                    a[i - cursor] = aar[i];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = op.apply(AbstractByteStream.this.toArray());
                    len = aar.length;
                }
            }
        }, sorted);
    }

    @Override
    public Stream<IndexedByte> indexed() {
        final MutableLong idx = MutableLong.of(0);

        return newStream(this.sequential().mapToObj(new ByteFunction<IndexedByte>() {
            @Override
            public IndexedByte apply(byte t) {
                return IndexedByte.of(t, idx.getAndIncrement());
            }
        }).iteratorEx(), true, INDEXED_BYTE_COMPARATOR);
    }

    @Override
    public ByteStream append(ByteStream stream) {
        return ByteStream.concat(this, stream);
    }

    @Override
    public ByteStream prepend(ByteStream stream) {
        return ByteStream.concat(stream, this);
    }

    @Override
    public ByteStream merge(ByteStream b, ByteBiFunction<Nth> nextSelector) {
        return ByteStream.merge(this, b, nextSelector);
    }

    @Override
    public ByteStream zipWith(ByteStream b, ByteBinaryOperator zipFunction) {
        return ByteStream.zip(this, b, zipFunction);
    }

    @Override
    public ByteStream zipWith(ByteStream b, ByteStream c, ByteTernaryOperator zipFunction) {
        return ByteStream.zip(this, b, c, zipFunction);
    }

    @Override
    public ByteStream zipWith(ByteStream b, byte valueForNoneA, byte valueForNoneB, ByteBinaryOperator zipFunction) {
        return ByteStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public ByteStream zipWith(ByteStream b, ByteStream c, byte valueForNoneA, byte valueForNoneB, byte valueForNoneC, ByteTernaryOperator zipFunction) {
        return ByteStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    @Override
    public <K, V> Map<K, V> toMap(ByteFunction<? extends K> keyMapper, ByteFunction<? extends V> valueMapper) {
        return toMap(keyMapper, valueMapper, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(ByteFunction<? extends K> keyMapper, ByteFunction<? extends V> valueMapper, Supplier<? extends M> mapFactory) {
        return toMap(keyMapper, valueMapper, Fn.<V> throwingMerger(), mapFactory);
    }

    @Override
    public <K, V> Map<K, V> toMap(ByteFunction<? extends K> keyMapper, ByteFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, A, D> Map<K, D> toMap(ByteFunction<? extends K> keyMapper, Collector<Byte, A, D> downstream) {
        return toMap(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    @Override
    public OptionalByte first() {
        assertNotClosed();

        try {
            final ByteIterator iter = this.iteratorEx();

            return iter.hasNext() ? OptionalByte.of(iter.nextByte()) : OptionalByte.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalByte last() {
        assertNotClosed();

        try {
            final ByteIterator iter = this.iteratorEx();

            if (iter.hasNext() == false) {
                return OptionalByte.empty();
            }

            byte next = iter.nextByte();

            while (iter.hasNext()) {
                next = iter.nextByte();
            }

            return OptionalByte.of(next);
        } finally {
            close();
        }
    }

    @Override
    public OptionalByte onlyOne() throws DuplicatedResultException {
        assertNotClosed();

        try {
            final ByteIterator iter = this.iteratorEx();

            final OptionalByte result = iter.hasNext() ? OptionalByte.of(iter.nextByte()) : OptionalByte.empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new DuplicatedResultException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.nextByte()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalByte findAny(final Try.BytePredicate<E> predicate) throws E {
        return findFirst(predicate);
    }

    @Override
    public <E extends Exception, E2 extends Exception> OptionalByte findFirstOrLast(Try.BytePredicate<E> predicateForFirst,
            Try.BytePredicate<E> predicateForLast) throws E, E2 {
        assertNotClosed();

        try {
            final ByteIteratorEx iter = iteratorEx();
            MutableByte last = null;
            byte next = 0;

            while (iter.hasNext()) {
                next = iter.nextByte();

                if (predicateForFirst.test(next)) {
                    return OptionalByte.of(next);
                } else if (predicateForLast.test(next)) {
                    if (last == null) {
                        last = MutableByte.of(next);
                    } else {
                        last.setValue(next);
                    }
                }
            }

            return last == null ? OptionalByte.empty() : OptionalByte.of(last.value());
        } finally {
            close();
        }
    }

    @Override
    public Optional<Map<Percentage, Byte>> percentiles() {
        assertNotClosed();

        try {
            final byte[] a = sorted().toArray();

            if (a.length == 0) {
                return Optional.empty();
            }

            return Optional.of(N.percentiles(a));
        } finally {
            close();
        }
    }

    @Override
    public Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> summarizeAndPercentiles() {
        assertNotClosed();

        try {
            final byte[] a = sorted().toArray();

            if (N.isNullOrEmpty(a)) {
                return Pair.of(new ByteSummaryStatistics(), Optional.<Map<Percentage, Byte>> empty());
            } else {
                return Pair.of(new ByteSummaryStatistics(a.length, sum(a), a[0], a[a.length - 1]), Optional.of(N.percentiles(a)));
            }
        } finally {
            close();
        }
    }

    @Override
    public String join(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) {
        assertNotClosed();

        try {
            final Joiner joiner = Joiner.with(delimiter, prefix, suffix).reuseCachedBuffer(true);
            final ByteIteratorEx iter = this.iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextByte());
            }

            return joiner.toString();
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjByteConsumer<? super R> accumulator) {
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }
}

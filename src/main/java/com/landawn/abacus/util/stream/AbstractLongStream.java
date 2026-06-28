/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.stream;

import static com.landawn.abacus.util.function.ToLongFunction.UNBOX;

import java.util.Collection;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IndexedLong;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongBiPredicate;
import com.landawn.abacus.util.function.LongMapMultiConsumer;
import com.landawn.abacus.util.function.LongTernaryOperator;
import com.landawn.abacus.util.function.LongTriPredicate;

/**
 * Abstract base implementation of LongStream providing common functionality for long stream operations.
 * This class serves as the foundation for concrete long stream implementations, offering intermediate
 * and terminal operations on sequences of primitive long values.
 *
 * <p>This implementation includes:
 * <ul>
 * <li>Rate limiting and delay operations for controlling stream processing speed</li>
 * <li>Filtering, mapping, and transformation operations</li>
 * <li>Collection and aggregation operations</li>
 * <li>Sorting, reversing, and rotation operations</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Subclasses implement concrete stream behavior
 * LongStream stream = new ArrayLongStream(new long[] {1L, 2L, 3L});
 * stream.filter(l -> l > 1L).forEach(System.out::println);
 * }</pre>
 *
 */
abstract class AbstractLongStream extends LongStream {

    /**
     * Constructs an AbstractLongStream with the specified sorting state and close handlers.
     *
     * @param sorted whether the stream elements are in sorted order
     * @param closeHandlers collection of handlers to execute when the stream is closed
     */
    AbstractLongStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);
    }

    @Override
    public LongStream rateLimited(final RateLimiter rateLimiter) throws IllegalArgumentException {
        checkArgNotNull(rateLimiter, cs.rateLimiter);

        final LongConsumer action = it -> rateLimiter.acquire();

        if (isParallel()) {
            //noinspection resource
            return sequential().onEach(action).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return onEach(action);
        }
    }

    @Override
    public LongStream delay(final Duration delay) throws IllegalArgumentException {
        checkArgNotNull(delay, cs.delay);

        final long millis = delay.toMillis();

        final LongConsumer action = new LongConsumer() {
            private boolean isFirst = true;

            @Override
            public void accept(final long it) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    N.sleepUninterruptibly(millis);
                }
            }
        };

        if (isParallel()) {
            //noinspection resource
            return sequential().onEach(action).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return onEach(action);
        }
    }

    @Override
    public LongStream debounce(Duration duration) {
        checkArgNotNull(duration, cs.duration);
        checkArgPositive(duration.toMillis(), cs.duration);

        if (isParallel()) {
            return psp(s -> s.debounce(duration));
        }

        final LongIteratorEx iter = iteratorEx();

        return newStream(new LongIteratorEx() {
            private final long durationMillis = duration.toMillis();
            private long prev = 0; // the most recent element of the current burst, awaiting a quiet gap
            private boolean hasPrev = false;
            private long prevTime = 0;
            private long next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                if (hasNext) {
                    return true;
                }

                while (iter.hasNext()) {
                    final long val = iter.nextLong();
                    final long now = System.currentTimeMillis();

                    if (!hasPrev) {
                        prev = val;
                        prevTime = now;
                        hasPrev = true;
                    } else if (now - prevTime >= durationMillis) {
                        // prev was followed by a quiet gap >= duration -> emit it; val starts the next burst.
                        next = prev;
                        hasNext = true;
                        prev = val;
                        prevTime = now;
                        return true;
                    } else {
                        // val arrived within the quiet window -> it supersedes prev.
                        prev = val;
                        prevTime = now;
                    }
                }

                // Source exhausted: the most recent pending element is always emitted.
                if (hasPrev) {
                    next = prev;
                    hasNext = true;
                    hasPrev = false;
                    return true;
                }

                return false;
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;
                return next;
            }
        }, isSorted());
    }

    @Override
    public LongStream skipUntil(final LongPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return dropWhile(t -> !predicate.test(t));
    }

    @Override
    public LongStream distinct() throws IllegalStateException {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        //noinspection resource
        return newStream(sequential().filter(set::add).iteratorEx(), isSorted());
    }

    @Override
    public LongStream flatMapArray(final LongFunction<long[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMap(t -> LongStream.of(mapper.apply(t)));
    }

    @Override
    public LongStream flattMap(final LongFunction<? extends java.util.stream.LongStream> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMap(t -> LongStream.from(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatmapToObj(final LongFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatMapArrayToObj(final LongFunction<T[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public LongStream mapMulti(final LongMapMultiConsumer mapper) {
        final LongFunction<LongStream> secondMapper = t -> {
            final SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong();

            mapper.accept(t, buffer);

            return LongStream.of(buffer.iterator());
        };

        return flatMap(secondMapper);
    }

    @Override
    public LongStream mapPartial(final LongFunction<OptionalLong> mapper) {
        if (isParallel()) {
            //noinspection resource
            return mapToObj(mapper).psp(s -> s.filter(Fn.IS_PRESENT_LONG).mapToLong(Fn.GET_AS_LONG));
        } else {
            //noinspection resource
            return mapToObj(mapper).filter(Fn.IS_PRESENT_LONG).mapToLong(Fn.GET_AS_LONG);
        }
    }

    @Override
    public LongStream mapPartialJdk(final LongFunction<java.util.OptionalLong> mapper) {
        if (isParallel()) {
            //noinspection resource
            return mapToObj(mapper).psp(s -> s.filter(Fn.IS_PRESENT_LONG_JDK).mapToLong(Fn.GET_AS_LONG_JDK));
        } else {
            //noinspection resource
            return mapToObj(mapper).filter(Fn.IS_PRESENT_LONG_JDK).mapToLong(Fn.GET_AS_LONG_JDK);
        }
    }

    @Override
    public LongStream rangeMap(final LongBiPredicate sameRange, final LongBinaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new LongIteratorEx() { //NOSONAR
            private long left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public long nextLong() {
                left = hasNext ? next : iter.nextLong();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextLong();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.applyAsLong(left, right);
            }
        }, false);
    }

    @Override
    public <T> Stream<T> rangeMapToObj(final LongBiPredicate sameRange, final LongBiFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private long left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                left = hasNext ? next : iter.nextLong();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextLong();

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
    public Stream<LongList> collapse(final LongBiPredicate collapsible) throws IllegalStateException {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private long next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public LongList next() {
                final LongList result = new LongList(9);
                result.add(hasNext ? next : (next = iter.nextLong()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextLong()))) {
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
    public LongStream collapse(final LongBiPredicate collapsible, final LongBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new LongIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private long next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public long nextLong() {
                long res = hasNext ? next : (next = iter.nextLong());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextLong()))) {
                        res = mergeFunction.applyAsLong(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public LongStream collapse(final LongTriPredicate collapsible, final LongBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new LongIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private long next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public long nextLong() {
                final long first = hasNext ? next : (next = iter.nextLong());
                long res = first;

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.nextLong()))) {
                        res = mergeFunction.applyAsLong(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public LongStream skip(final long n, final LongConsumer action) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);
        checkArgNotNull(action, cs.action);

        if (n == 0) {
            return this;
        }

        final LongPredicate filter = isParallel() ? new LongPredicate() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(final long value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new LongPredicate() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(final long value) throws IllegalStateException {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public LongStream filter(final LongPredicate predicate, final LongConsumer onDrop) throws IllegalStateException {
        assertNotClosed();

        return filter(value -> {
            if (!predicate.test(value)) {
                onDrop.accept(value);
                return false;
            }

            return true;
        });
    }

    @Override
    public LongStream dropWhile(final LongPredicate predicate, final LongConsumer onDrop) throws IllegalStateException {
        assertNotClosed();

        return dropWhile(value -> {
            if (predicate.test(value)) {
                onDrop.accept(value);
                return true;
            }

            return false;
        });
    }

    @Override
    public LongStream step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1) {
            return skip(0);
        }

        final long skip = step - 1;
        final LongIteratorEx iter = iteratorEx();

        final LongIterator longIterator = new LongIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                final long next = iter.nextLong();
                iter.advance(skip);
                return next;
            }
        };

        return newStream(longIterator, isSorted());
    }

    @Override
    public LongStream scan(final LongBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new LongIteratorEx() { //NOSONAR
            private long res = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (isFirst) {
                    isFirst = false;
                    return (res = iter.nextLong());
                } else {
                    return (res = accumulator.applyAsLong(res, iter.nextLong()));
                }
            }
        }, false);
    }

    @Override
    public LongStream scan(final long init, final LongBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new LongIteratorEx() { //NOSONAR
            private long res = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                return (res = accumulator.applyAsLong(res, iter.nextLong()));
            }
        }, false);
    }

    @Override
    public LongStream scan(final long init, final boolean initIncluded, final LongBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (!initIncluded) {
            return scan(init, accumulator);
        }

        final LongIteratorEx iter = iteratorEx();

        return newStream(new LongIteratorEx() { //NOSONAR
            private boolean isFirst = true;
            private long res = init;

            @Override
            public boolean hasNext() {
                return isFirst || iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (res = accumulator.applyAsLong(res, iter.nextLong()));
            }
        }, false);
    }

    @Override
    public LongStream top(final int n) throws IllegalStateException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return limit(0);
        }

        return top(n, LONG_COMPARATOR);
    }

    @Override
    public LongStream intersection(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(multiset::remove).iteratorEx(), isSorted());
    }

    @Override
    public LongStream difference(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value)).iteratorEx(), isSorted());
    }

    @Override
    public LongStream symmetricDifference(final Collection<? extends Long> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value)).append(Stream.of(c).filter(multiset::remove).mapToLong(UNBOX)).iteratorEx(),
                false);
    }

    @Override
    public LongStream reversed() throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private long[] elements;
            private int fromIndex = -1;
            private int toIndex = -1;

            private int cursor;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cursor > fromIndex;
            }

            @Override
            public long nextLong() {
                if (!initialized) {
                    init();
                }

                if (cursor <= fromIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return elements[--cursor];
            }

            @Override
            public long count() {
                if (!initialized) {
                    init();
                }

                final long ret = cursor - fromIndex;
                cursor = fromIndex;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (!initialized) {
                    init();
                }

                cursor = n < cursor - fromIndex ? cursor - (int) n : fromIndex;
            }

            @Override
            public long[] toArray() {
                if (!initialized) {
                    init();
                }

                final long[] a = new long[cursor - fromIndex];

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = elements[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<long[], Integer, Integer> tp = AbstractLongStream.this.arrayForIntermediateOp();

                    elements = tp._1;
                    fromIndex = tp._2;
                    toIndex = tp._3;

                    cursor = toIndex;
                }
            }
        }, false);
    }

    @Override
    public LongStream rotated(final int distance) throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private long[] elements;
            private int fromIndex = -1;
            private int toIndex = -1;

            private int len;
            private int start;
            private int cnt = 0;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cnt < len;
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return elements[((start + cnt++) % len) + fromIndex];
            }

            @Override
            public long count() {
                if (!initialized) {
                    init();
                }

                final long ret = len - cnt;
                cnt = len;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (!initialized) {
                    init();
                }

                cnt = n < len - cnt ? cnt + (int) n : len;
            }

            @Override
            public long[] toArray() {
                if (!initialized) {
                    init();
                }

                final long[] a = new long[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = elements[((start + i) % len) + fromIndex];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<long[], Integer, Integer> tp = AbstractLongStream.this.arrayForIntermediateOp();

                    elements = tp._1;
                    fromIndex = tp._2;
                    toIndex = tp._3;

                    len = toIndex - fromIndex;

                    if (len > 0) {
                        start = distance % len;

                        if (start < 0) {
                            start += len;
                        }

                        start = len - start;
                    }
                }
            }
        }, distance == 0 && isSorted());
    }

    @Override
    public LongStream shuffled(final Random rnd) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(rnd, cs.rnd);

        return lazyLoad(a -> {
            N.shuffle(a, rnd);
            return a;
        }, false);
    }

    @Override
    public LongStream sorted() throws IllegalStateException {
        assertNotClosed();

        if (isSorted()) {
            // return newStream(iteratorEx(), isSorted());
            return this;
        }

        return lazyLoad(a -> {
            if (isParallel()) {
                N.parallelSort(a);
            } else {
                N.sort(a);
            }

            return a;
        }, true);
    }

    @Override
    public LongStream reverseSorted() throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            private boolean initialized = false;
            private long[] sortedArray;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public long nextLong() {
                if (!initialized) {
                    init();
                }

                if (cursor <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return sortedArray[--cursor];
            }

            @Override
            public long count() {
                if (!initialized) {
                    init();
                }

                final long ret = cursor;
                cursor = 0;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (!initialized) {
                    init();
                }

                cursor = n < cursor ? cursor - (int) n : 0;
            }

            @Override
            public long[] toArray() {
                if (!initialized) {
                    init();
                }

                final long[] a = new long[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = sortedArray[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    sortedArray = AbstractLongStream.this.toArrayForIntermediateOp();

                    if (isParallel()) {
                        N.parallelSort(sortedArray);
                    } else {
                        N.sort(sortedArray);
                    }

                    cursor = sortedArray.length;
                }
            }
        }, false);
    }

    /**
     * Creates a lazily-loaded LongStream by applying the given array transformation operation.
     * The stream materializes all elements into an array and applies {@code op} when the returned
     * stream is first consumed.
     *
     * @param op the transformation to apply to the collected element array
     * @param sorted whether the resulting stream should be marked as sorted
     * @return a new LongStream backed by the transformed array
     */
    private LongStream lazyLoad(final UnaryOperator<long[]> op, final boolean sorted) {
        return LongStream.defer(() -> newStream(op.apply(toArrayForIntermediateOp()), sorted)).onClose(this::close);
    }

    @Override
    public LongStream cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIterator() { //NOSONAR
            private LongIterator iter = null;
            private LongList list = null;
            private long[] a = null;
            private int len = 0;
            private int cursor = -1;
            private long currentElement = 0;

            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                if (a != null) {
                    return len > 0;
                } else if (iter.hasNext()) {
                    return true;
                } else {
                    a = list.toArray();
                    len = a.length;
                    cursor = 0;

                    return len > 0;
                }
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (len > 0) {
                    if (cursor >= len) {
                        cursor = 0;
                    }

                    return a[cursor++];
                } else {
                    currentElement = iter.nextLong();
                    list.add(currentElement);

                    return currentElement;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractLongStream.this.iteratorEx();
                    list = new LongList();
                }
            }
        }, false);
    }

    @Override
    public LongStream cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new LongIterator() { //NOSONAR
            private LongIterator iter = null;
            private LongList list = null;
            private long[] a = null;
            private int len = 0;
            private int cursor = -1;
            private long currentElement = 0;
            private long roundsCompleted = 0;

            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                if (roundsCompleted >= rounds) {
                    return false;
                }

                if (a != null) {
                    // len == 0 means the source produced zero elements; cycling an empty
                    // stream any number of times yields nothing — must short-circuit here,
                    // otherwise rounds >= 3 would have hasNext() return true while nextLong()
                    // throws NoSuchElementException on the exhausted iter.
                    return len > 0 && (cursor < len || rounds - roundsCompleted > 1);
                } else if (iter.hasNext()) {
                    return true;
                } else {
                    a = list.toArray();
                    len = a.length;
                    cursor = 0;
                    roundsCompleted++;

                    return roundsCompleted < rounds && len > 0;
                }
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (len > 0) {
                    if (cursor >= len) {
                        cursor = 0;
                        roundsCompleted++;
                    }

                    return a[cursor++];
                } else {
                    currentElement = iter.nextLong();
                    list.add(currentElement);

                    return currentElement;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractLongStream.this.iteratorEx();
                    list = new LongList();
                }
            }
        }, rounds <= 1 && isSorted());
    }

    @Override
    public Stream<IndexedLong> indexed() throws IllegalStateException {
        assertNotClosed();

        final MutableLong idx = MutableLong.of(0);

        //noinspection resource
        return newStream(sequential().mapToObj(t -> IndexedLong.of(t, idx.getAndIncrement())).iteratorEx(), true, INDEXED_LONG_COMPARATOR);
    }

    @Override
    public Stream<Long> boxed() throws IllegalStateException {
        assertNotClosed();

        return newStream(iteratorEx(), isSorted(), isSorted() ? LONG_COMPARATOR : null);
    }

    @Override
    public final LongStream prepend(final long... a) throws IllegalStateException {
        assertNotClosed();

        return prepend(LongStream.of(a));
    }

    @Override
    public LongStream prepend(final LongStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return LongStream.concat(stream, this).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return LongStream.concat(stream, this);
        }
    }

    @Override
    public LongStream prepend(final OptionalLong op) throws IllegalStateException {
        assertNotClosed();

        // return prepend(op.stream());
        return op.isEmpty() ? this : prepend(op.orElseThrow());
    }

    @Override
    public final LongStream append(final long... a) throws IllegalStateException {
        assertNotClosed();

        return append(LongStream.of(a));
    }

    @Override
    public LongStream append(final LongStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return LongStream.concat(this, stream).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return LongStream.concat(this, stream);
        }
    }

    @Override
    public LongStream append(final OptionalLong op) { //NOSONAR
        assertNotClosed();

        // return append(op.stream());
        return op.isEmpty() ? this : append(op.orElseThrow());
    }

    @Override
    public final LongStream appendIfEmpty(final long... a) throws IllegalStateException {
        assertNotClosed();

        return appendIfEmpty(() -> LongStream.of(a));
    }

    @Override
    public LongStream mergeWith(final LongStream b, final LongBiFunction<MergeResult> nextSelector) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return LongStream.merge(this, b, nextSelector).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return LongStream.merge(this, b, nextSelector);
        }
    }

    @Override
    public LongStream zipWith(final LongStream b, final LongBinaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return LongStream.zip(this, b, zipFunction);
    }

    @Override
    public LongStream zipWith(final LongStream b, final LongStream c, final LongTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return LongStream.zip(this, b, c, zipFunction);
    }

    @Override
    public LongStream zipWith(final LongStream b, final long valueForNoneA, final long valueForNoneB, final LongBinaryOperator zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        return LongStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public LongStream zipWith(final LongStream b, final LongStream c, final long valueForNoneA, final long valueForNoneB, final long valueForNoneC,
            final LongTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return LongStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.LongFunction<? extends K, E> keyMapper,
            final Throwables.LongFunction<? extends V, E2> valueMapper) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.LongFunction<? extends K, E> keyMapper,
            final Throwables.LongFunction<? extends V, E2> valueMapper, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Fn.throwingMerger(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.LongFunction<? extends K, E> keyMapper,
            final Throwables.LongFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    @Override
    public <K, D, E extends Exception> Map<K, D> groupTo(final Throwables.LongFunction<? extends K, E> keyMapper,
            final Collector<? super Long, ?, D> downstream) throws E {
        assertNotClosed();

        return groupTo(keyMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <E extends Exception> void forEachIndexed(final Throwables.IntLongConsumer<E> action) throws IllegalStateException, E {
        assertNotClosed();

        if (isParallel()) {
            final AtomicInteger idx = new AtomicInteger(0);

            forEach(t -> action.accept(idx.getAndIncrement(), t));
        } else {
            final MutableInt idx = MutableInt.of(0);

            forEach(t -> action.accept(idx.getAndIncrement(), t));
        }
    }

    @Override
    public OptionalLong first() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final LongIterator iter = iteratorEx();

            return iter.hasNext() ? OptionalLong.of(iter.nextLong()) : OptionalLong.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalLong last() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final LongIterator iter = iteratorEx();

            if (!iter.hasNext()) {
                return OptionalLong.empty();
            }

            long next = iter.nextLong();

            while (iter.hasNext()) {
                next = iter.nextLong();
            }

            return OptionalLong.of(next);
        } finally {
            close();
        }
    }

    @SuppressWarnings("DuplicateThrows")
    @Override
    public OptionalLong onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final LongIterator iter = iteratorEx();

            final OptionalLong result = iter.hasNext() ? OptionalLong.of(iter.nextLong()) : OptionalLong.empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new TooManyElementsException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.nextLong()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalLong findAny(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        return findFirst(predicate);
    }

    @Override
    public Optional<Map<Percentage, Long>> percentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final long[] a = sorted().toArray();

            if (a.length == 0) {
                return Optional.empty();
            }

            return Optional.of(N.percentilesOfSorted(a));
        } finally {
            close();
        }
    }

    @Override
    public Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> summaryStatisticsAndPercentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final long[] a = sorted().toArray();

            if (N.isEmpty(a)) {
                return Pair.of(new LongSummaryStatistics(), Optional.empty());
            } else {
                return Pair.of(new LongSummaryStatistics(a.length, a[0], a[a.length - 1], sum(a)), Optional.of(N.percentilesOfSorted(a)));
            }
        } finally {
            close();
        }
    }

    @Override
    public String join(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final Joiner joiner = Joiner.with(delimiter, prefix, suffix).reuseBuffer();
            @SuppressWarnings("resource")
            final LongIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextLong());
            }

            return joiner.toString();
        } finally {
            close();
        }
    }

    @Override
    public Joiner joinTo(final Joiner joiner) throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final LongIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextLong());
            }
        } finally {
            close();
        }

        return joiner;
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjLongConsumer<? super R> accumulator) throws IllegalStateException {
        assertNotClosed();

        @SuppressWarnings("UnnecessaryLocalVariable")
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }

    @Override
    public LongIterator iterator() throws IllegalStateException {
        assertNotClosed();

        if (!isEmptyCloseHandlers(closeHandlers()) && logger.isWarnEnabled()) {
            logger.warn("Remember to close {} after iteration because it has close handlers", ClassUtil.getSimpleClassName(getClass()));
        }

        return iteratorEx();
    }
}

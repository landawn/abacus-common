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

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IndexedShort;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ObjShortConsumer;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortBiPredicate;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortTernaryOperator;
import com.landawn.abacus.util.function.ShortTriPredicate;
import com.landawn.abacus.util.function.ToShortFunction;

/**
 * Abstract base implementation of ShortStream providing common functionality for short stream operations.
 * This class serves as the foundation for concrete short stream implementations, offering intermediate
 * and terminal operations on sequences of primitive short values.
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
 * ShortStream stream = new ArrayShortStream(new short[] {1, 2, 3});
 * stream.filter(s -> s > 1).forEach(System.out::println);
 * }</pre>
 *
 */
abstract class AbstractShortStream extends ShortStream {

    /**
     * Constructs an AbstractShortStream with the specified sorting state and close handlers.
     *
     * @param sorted whether the stream elements are in sorted order
     * @param closeHandlers collection of handlers to execute when the stream is closed
     */
    AbstractShortStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);
    }

    @Override
    public ShortStream rateLimited(final RateLimiter rateLimiter) throws IllegalArgumentException {
        checkArgNotNull(rateLimiter, cs.rateLimiter);

        final ShortConsumer action = it -> rateLimiter.acquire();

        if (isParallel()) {
            //noinspection resource
            return sequential().onEach(action).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return onEach(action);
        }
    }

    @Override
    public ShortStream delay(final Duration delay) throws IllegalArgumentException {
        checkArgNotNull(delay, cs.delay);

        final long millis = delay.toMillis();

        final ShortConsumer action = new ShortConsumer() {
            private boolean isFirst = true;

            @Override
            public void accept(final short it) {
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
    public ShortStream debounce(Duration duration) {
        checkArgNotNull(duration, cs.duration);
        checkArgPositive(duration.toMillis(), cs.duration);

        if (isParallel()) {
            return psp(s -> s.debounce(duration));
        }

        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ShortIteratorEx() {
            private final long durationMillis = duration.toMillis();
            private short prev = 0; // the most recent element of the current burst, awaiting a quiet gap
            private boolean hasPrev = false;
            private long prevTime = 0;
            private short next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                if (hasNext) {
                    return true;
                }

                while (iter.hasNext()) {
                    final short val = iter.nextShort();
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
            public short nextShort() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;
                return next;
            }
        }, isSorted());
    }

    @Override
    public ShortStream skipUntil(final ShortPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return dropWhile(t -> !predicate.test(t));
    }

    @Override
    public ShortStream distinct() throws IllegalStateException {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        //noinspection resource
        return newStream(sequential().filter(set::add).iteratorEx(), isSorted());
    }

    @Override
    public ShortStream flatMapArray(final ShortFunction<short[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMap(t -> ShortStream.of(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatmapToObj(final ShortFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatMapArrayToObj(final ShortFunction<T[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public ShortStream mapPartial(final ShortFunction<OptionalShort> mapper) {
        if (isParallel()) {
            //noinspection resource
            return mapToObj(mapper).psp(s -> s.filter(Fn.IS_PRESENT_SHORT).mapToShort(Fn.GET_AS_SHORT));
        } else {
            //noinspection resource
            return mapToObj(mapper).filter(Fn.IS_PRESENT_SHORT).mapToShort(Fn.GET_AS_SHORT);
        }
    }

    @Override
    public ShortStream rangeMap(final ShortBiPredicate sameRange, final ShortBinaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private short left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public short nextShort() {
                left = hasNext ? next : iter.nextShort();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextShort();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.applyAsShort(left, right);
            }
        }, false);
    }

    @Override
    public <T> Stream<T> rangeMapToObj(final ShortBiPredicate sameRange, final ShortBiFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private short left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                left = hasNext ? next : iter.nextShort();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextShort();

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
    public Stream<ShortList> collapse(final ShortBiPredicate collapsible) throws IllegalStateException {
        assertNotClosed();

        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private short next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public ShortList next() {
                final ShortList result = new ShortList(9);
                result.add(hasNext ? next : (next = iter.nextShort()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextShort()))) {
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
    public ShortStream collapse(final ShortBiPredicate collapsible, final ShortBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private short next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public short nextShort() {
                short merged = hasNext ? next : (next = iter.nextShort());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextShort()))) {
                        merged = mergeFunction.applyAsShort(merged, next);
                    } else {
                        break;
                    }
                }

                return merged;
            }
        }, false);
    }

    @Override
    public ShortStream collapse(final ShortTriPredicate collapsible, final ShortBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private short next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public short nextShort() {
                final short first = hasNext ? next : (next = iter.nextShort());
                short merged = first;

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.nextShort()))) {
                        merged = mergeFunction.applyAsShort(merged, next);
                    } else {
                        break;
                    }
                }

                return merged;
            }
        }, false);
    }

    @Override
    public ShortStream skip(final long n, final ShortConsumer action) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);
        checkArgNotNull(action, cs.action);

        if (n == 0) {
            return this;
        }

        final ShortPredicate filter = isParallel() ? new ShortPredicate() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(final short value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new ShortPredicate() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(final short value) throws IllegalStateException {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public ShortStream filter(final ShortPredicate predicate, final ShortConsumer onDrop) throws IllegalStateException {
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
    public ShortStream dropWhile(final ShortPredicate predicate, final ShortConsumer onDrop) throws IllegalStateException {
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
    public ShortStream step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1) {
            return skip(0);
        }

        final long skip = step - 1;
        final ShortIteratorEx iter = iteratorEx();

        final ShortIterator shortIterator = new ShortIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public short nextShort() {
                final short next = iter.nextShort();
                iter.advance(skip);
                return next;
            }
        };

        return newStream(shortIterator, isSorted());
    }

    @Override
    public ShortStream scan(final ShortBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private short accumulated = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public short nextShort() {
                if (isFirst) {
                    isFirst = false;
                    return (accumulated = iter.nextShort());
                } else {
                    return (accumulated = accumulator.applyAsShort(accumulated, iter.nextShort()));
                }
            }
        }, false);
    }

    @Override
    public ShortStream scan(final short init, final ShortBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private short accumulated = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public short nextShort() {
                return (accumulated = accumulator.applyAsShort(accumulated, iter.nextShort()));
            }
        }, false);
    }

    @Override
    public ShortStream scan(final short init, final boolean initIncluded, final ShortBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (!initIncluded) {
            return scan(init, accumulator);
        }

        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private boolean isFirst = true;
            private short accumulated = init;

            @Override
            public boolean hasNext() {
                return isFirst || iter.hasNext();
            }

            @Override
            public short nextShort() {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (accumulated = accumulator.applyAsShort(accumulated, iter.nextShort()));
            }
        }, false);
    }

    @Override
    public ShortStream top(final int n) throws IllegalStateException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return limit(0);
        }

        return top(n, SHORT_COMPARATOR);
    }

    @Override
    public ShortStream intersection(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(multiset::remove).iteratorEx(), isSorted());
    }

    @Override
    public ShortStream difference(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value)).iteratorEx(), isSorted());
    }

    @Override
    public ShortStream symmetricDifference(final Collection<? extends Short> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value))
                .append(Stream.of(c).filter(multiset::remove).mapToShort(ToShortFunction.UNBOX))
                .iteratorEx(), false);
    }

    @Override
    public ShortStream reversed() throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private short[] elements;
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
            public short nextShort() {
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
            public short[] toArray() {
                if (!initialized) {
                    init();
                }

                final short[] a = new short[cursor - fromIndex];

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = elements[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<short[], Integer, Integer> tp = AbstractShortStream.this.arrayForIntermediateOp();

                    elements = tp._1;
                    fromIndex = tp._2;
                    toIndex = tp._3;

                    cursor = toIndex;
                }
            }
        }, false);
    }

    @Override
    public ShortStream rotated(final int distance) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private short[] elements;
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
            public short nextShort() {
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
            public short[] toArray() {
                if (!initialized) {
                    init();
                }

                final short[] a = new short[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = elements[((start + i) % len) + fromIndex];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<short[], Integer, Integer> tp = AbstractShortStream.this.arrayForIntermediateOp();

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
    public ShortStream shuffled(final Random rnd) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(rnd, cs.rnd);

        return lazyLoad(a -> {
            N.shuffle(a, rnd);
            return a;
        }, false);
    }

    @Override
    public ShortStream sorted() throws IllegalStateException {
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
    public ShortStream reverseSorted() throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private boolean initialized = false;
            private short[] sortedArray;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public short nextShort() {
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
            public short[] toArray() {
                if (!initialized) {
                    init();
                }

                final short[] a = new short[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = sortedArray[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    sortedArray = AbstractShortStream.this.toArrayForIntermediateOp();

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
     * Creates a lazily-loaded ShortStream by applying the given array transformation operation.
     * The stream materializes all elements into an array and applies {@code op} when the returned
     * stream is first consumed.
     *
     * @param op the transformation to apply to the collected element array
     * @param sorted whether the resulting stream should be marked as sorted
     * @return a new ShortStream backed by the transformed array
     */
    private ShortStream lazyLoad(final UnaryOperator<short[]> op, final boolean sorted) {
        return ShortStream.defer(() -> newStream(op.apply(toArrayForIntermediateOp()), sorted)).onClose(this::close);
    }

    @Override
    public ShortStream cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIterator() { //NOSONAR
            private ShortIterator iter = null;
            private ShortList list = null;
            private short[] a = null;
            private int len = 0;
            private int cursor = -1;
            private short currentElement = 0;

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
            public short nextShort() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (len > 0) {
                    if (cursor >= len) {
                        cursor = 0;
                    }

                    return a[cursor++];
                } else {
                    currentElement = iter.nextShort();
                    list.add(currentElement);

                    return currentElement;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractShortStream.this.iteratorEx();
                    list = new ShortList();
                }
            }
        }, false);
    }

    @Override
    public ShortStream cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new ShortIterator() { //NOSONAR
            private ShortIterator iter = null;
            private ShortList list = null;
            private short[] a = null;
            private int len = 0;
            private int cursor = -1;
            private short currentElement = 0;
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
                    // otherwise rounds >= 3 would have hasNext() return true while nextShort()
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
            public short nextShort() {
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
                    currentElement = iter.nextShort();
                    list.add(currentElement);

                    return currentElement;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractShortStream.this.iteratorEx();
                    list = new ShortList();
                }
            }
        }, rounds <= 1 && isSorted());
    }

    @Override
    public Stream<IndexedShort> indexed() throws IllegalStateException {
        assertNotClosed();

        //noinspection resource
        return newStream(sequential().mapToObj(new ShortFunction<IndexedShort>() { //NOSONAR
            final MutableLong idx = MutableLong.of(0);

            @Override
            public IndexedShort apply(final short t) {
                return IndexedShort.of(t, idx.getAndIncrement());
            }
        }).iteratorEx(), true, INDEXED_SHORT_COMPARATOR);
    }

    @Override
    public Stream<Short> boxed() throws IllegalStateException {
        assertNotClosed();

        return newStream(iteratorEx(), isSorted(), isSorted() ? SHORT_COMPARATOR : null);
    }

    @Override
    public final ShortStream prepend(final short... a) throws IllegalStateException {
        assertNotClosed();

        return prepend(ShortStream.of(a));
    }

    @Override
    public ShortStream prepend(final ShortStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return ShortStream.concat(stream, this).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return ShortStream.concat(stream, this);
        }
    }

    @Override
    public ShortStream prepend(final OptionalShort op) throws IllegalStateException {
        assertNotClosed();

        // return prepend(op.stream());
        return op.isEmpty() ? this : prepend(op.orElseThrow());
    }

    @Override
    public final ShortStream append(final short... a) throws IllegalStateException {
        assertNotClosed();

        return append(ShortStream.of(a));
    }

    @Override
    public ShortStream append(final ShortStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return ShortStream.concat(this, stream).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return ShortStream.concat(this, stream);
        }
    }

    @Override
    public ShortStream append(final OptionalShort op) { //NOSONAR
        assertNotClosed();

        // return append(op.stream());
        return op.isEmpty() ? this : append(op.orElseThrow());
    }

    @Override
    public final ShortStream appendIfEmpty(final short... a) throws IllegalStateException {
        assertNotClosed();

        return appendIfEmpty(() -> ShortStream.of(a));
    }

    @Override
    public ShortStream mergeWith(final ShortStream b, final ShortBiFunction<MergeResult> nextSelector) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return ShortStream.merge(this, b, nextSelector).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return ShortStream.merge(this, b, nextSelector);
        }
    }

    @Override
    public ShortStream zipWith(final ShortStream b, final ShortBinaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return ShortStream.zip(this, b, zipFunction);
    }

    @Override
    public ShortStream zipWith(final ShortStream b, final ShortStream c, final ShortTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return ShortStream.zip(this, b, c, zipFunction);
    }

    @Override
    public ShortStream zipWith(final ShortStream b, final short valueForNoneA, final short valueForNoneB, final ShortBinaryOperator zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        return ShortStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public ShortStream zipWith(final ShortStream b, final ShortStream c, final short valueForNoneA, final short valueForNoneB, final short valueForNoneC,
            final ShortTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return ShortStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.ShortFunction<? extends K, E> keyMapper,
            final Throwables.ShortFunction<? extends V, E2> valueMapper) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.ShortFunction<? extends K, E> keyMapper,
            final Throwables.ShortFunction<? extends V, E2> valueMapper, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Fn.throwingMerger(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.ShortFunction<? extends K, E> keyMapper,
            final Throwables.ShortFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    @Override
    public <K, D, E extends Exception> Map<K, D> groupTo(final Throwables.ShortFunction<? extends K, E> keyMapper,
            final Collector<? super Short, ?, D> downstream) throws E {
        assertNotClosed();

        return groupTo(keyMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <E extends Exception> void forEachIndexed(final Throwables.IntShortConsumer<E> action) throws IllegalStateException, E {
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
    public OptionalShort first() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final ShortIterator iter = iteratorEx();

            return iter.hasNext() ? OptionalShort.of(iter.nextShort()) : OptionalShort.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalShort last() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final ShortIterator iter = iteratorEx();

            if (!iter.hasNext()) {
                return OptionalShort.empty();
            }

            short next = iter.nextShort();

            while (iter.hasNext()) {
                next = iter.nextShort();
            }

            return OptionalShort.of(next);
        } finally {
            close();
        }
    }

    @SuppressWarnings("DuplicateThrows")
    @Override
    public OptionalShort onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final ShortIterator iter = iteratorEx();

            final OptionalShort result = iter.hasNext() ? OptionalShort.of(iter.nextShort()) : OptionalShort.empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new TooManyElementsException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.nextShort()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalShort findAny(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        return findFirst(predicate);
    }

    @Override
    public Optional<Map<Percentage, Short>> percentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final short[] a = sorted().toArray();

            if (a.length == 0) {
                return Optional.empty();
            }

            return Optional.of(N.percentilesOfSorted(a));
        } finally {
            close();
        }
    }

    @Override
    public Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> summaryStatisticsAndPercentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final short[] a = sorted().toArray();

            if (N.isEmpty(a)) {
                return Pair.of(new ShortSummaryStatistics(), Optional.empty());
            } else {
                // Compute sum as a long locally; StreamBase.sum(short[]) wraps the result in
                // toIntExact and throws ArithmeticException on overflow, but ShortSummaryStatistics's
                // 4th argument is a long and the analogous summaryStatistics() path accumulates
                // into a long without overflow. Diverging behavior here would cause one API to
                // throw on the same data the other handles cleanly.
                long s = 0;
                for (short v : a) {
                    s += v;
                }
                return Pair.of(new ShortSummaryStatistics(a.length, a[0], a[a.length - 1], s), Optional.of(N.percentilesOfSorted(a)));
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
            final ShortIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextShort());
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
            final ShortIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextShort());
            }
        } finally {
            close();
        }

        return joiner;
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjShortConsumer<? super R> accumulator) throws IllegalStateException {
        assertNotClosed();

        @SuppressWarnings("UnnecessaryLocalVariable")
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }

    @Override
    public ShortIterator iterator() throws IllegalStateException {
        assertNotClosed();

        if (!isEmptyCloseHandlers(closeHandlers()) && logger.isWarnEnabled()) {
            logger.warn("Remember to close {} after iteration because it has close handlers", ClassUtil.getSimpleClassName(getClass()));
        }

        return iteratorEx();
    }
}

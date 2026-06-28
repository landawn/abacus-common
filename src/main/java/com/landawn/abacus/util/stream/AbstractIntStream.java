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

import static com.landawn.abacus.util.function.ToIntFunction.UNBOX;

import java.util.Collection;
import java.util.IntSummaryStatistics;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IndexedInt;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Joiner;
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
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBiPredicate;
import com.landawn.abacus.util.function.IntMapMultiConsumer;
import com.landawn.abacus.util.function.IntTernaryOperator;
import com.landawn.abacus.util.function.IntTriPredicate;

/**
 * Abstract base implementation of IntStream providing common functionality for int stream operations.
 * This class serves as the foundation for concrete int stream implementations, offering intermediate
 * and terminal operations on sequences of primitive int values.
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
 * IntStream stream = new ArrayIntStream(new int[] {1, 2, 3});
 * stream.filter(i -> i > 1).forEach(System.out::println);
 * }</pre>
 *
 */
abstract class AbstractIntStream extends IntStream {

    /**
     * Constructs an AbstractIntStream with the specified sorting state and close handlers.
     *
     * @param sorted whether the stream elements are in sorted order
     * @param closeHandlers collection of handlers to execute when the stream is closed
     */
    AbstractIntStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);
    }

    @Override
    public IntStream rateLimited(final RateLimiter rateLimiter) throws IllegalArgumentException {
        checkArgNotNull(rateLimiter, cs.rateLimiter);

        final IntConsumer action = it -> rateLimiter.acquire();

        if (isParallel()) {
            //noinspection resource
            return sequential().onEach(action).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return onEach(action);
        }
    }

    @Override
    public IntStream delay(final Duration delay) throws IllegalArgumentException {
        checkArgNotNull(delay, cs.delay);

        final long millis = delay.toMillis();

        final IntConsumer action = new IntConsumer() {
            private boolean isFirst = true;

            @Override
            public void accept(final int it) {
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
    public IntStream debounce(Duration duration) {
        checkArgNotNull(duration, cs.duration);
        checkArgPositive(duration.toMillis(), cs.duration);

        if (isParallel()) {
            return psp(s -> s.debounce(duration));
        }

        final IntIteratorEx iter = iteratorEx();

        return newStream(new IntIteratorEx() {
            private final long durationMillis = duration.toMillis();
            private int prev = 0; // the most recent element of the current burst, awaiting a quiet gap
            private boolean hasPrev = false;
            private long prevTime = 0;
            private int next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                if (hasNext) {
                    return true;
                }

                while (iter.hasNext()) {
                    final int val = iter.nextInt();
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
            public int nextInt() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;
                return next;
            }
        }, isSorted());
    }

    @Override
    public IntStream skipUntil(final IntPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return dropWhile(t -> !predicate.test(t));
    }

    @Override
    public IntStream distinct() throws IllegalStateException {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        //noinspection resource
        return newStream(sequential().filter(set::add).iteratorEx(), isSorted());
    }

    @Override
    public IntStream flatMapArray(final IntFunction<int[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMap(t -> IntStream.of(mapper.apply(t)));
    }

    @Override
    public IntStream flattMap(final IntFunction<? extends java.util.stream.IntStream> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMap(t -> IntStream.from(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatmapToObj(final IntFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatMapArrayToObj(final IntFunction<T[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public IntStream mapMulti(final IntMapMultiConsumer mapper) {
        final IntFunction<IntStream> secondMapper = t -> {
            final SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt();

            mapper.accept(t, buffer);

            return IntStream.of(buffer.iterator());
        };

        return flatMap(secondMapper);
    }

    @Override
    public IntStream mapPartial(final IntFunction<OptionalInt> mapper) {
        if (isParallel()) {
            //noinspection resource
            return mapToObj(mapper).psp(s -> s.filter(Fn.IS_PRESENT_INT).mapToInt(Fn.GET_AS_INT));
        } else {
            //noinspection resource
            return mapToObj(mapper).filter(Fn.IS_PRESENT_INT).mapToInt(Fn.GET_AS_INT);
        }
    }

    @Override
    public IntStream mapPartialJdk(final IntFunction<java.util.OptionalInt> mapper) {
        if (isParallel()) {
            //noinspection resource
            return mapToObj(mapper).psp(s -> s.filter(Fn.IS_PRESENT_INT_JDK).mapToInt(Fn.GET_AS_INT_JDK));
        } else {
            //noinspection resource
            return mapToObj(mapper).filter(Fn.IS_PRESENT_INT_JDK).mapToInt(Fn.GET_AS_INT_JDK);
        }
    }

    @Override
    public IntStream rangeMap(final IntBiPredicate sameRange, final IntBinaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new IntIteratorEx() { //NOSONAR
            private int left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public int nextInt() {
                left = hasNext ? next : iter.nextInt();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextInt();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.applyAsInt(left, right);
            }
        }, false);
    }

    @Override
    public <T> Stream<T> rangeMapToObj(final IntBiPredicate sameRange, final IntBiFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                left = hasNext ? next : iter.nextInt();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextInt();

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
    public Stream<IntList> collapse(final IntBiPredicate collapsible) throws IllegalStateException {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private int next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public IntList next() {
                final IntList result = new IntList(9);
                result.add(hasNext ? next : (next = iter.nextInt()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextInt()))) {
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
    public IntStream collapse(final IntBiPredicate collapsible, final IntBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new IntIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private int next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public int nextInt() {
                int res = hasNext ? next : (next = iter.nextInt());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextInt()))) {
                        res = mergeFunction.applyAsInt(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public IntStream collapse(final IntTriPredicate collapsible, final IntBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new IntIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private int next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public int nextInt() {
                final int first = hasNext ? next : (next = iter.nextInt());
                int res = first;

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.nextInt()))) {
                        res = mergeFunction.applyAsInt(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public IntStream skip(final long n, final IntConsumer action) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);
        checkArgNotNull(action, cs.action);

        if (n == 0) {
            return this;
        }

        final IntPredicate filter = isParallel() ? new IntPredicate() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(final int value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new IntPredicate() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(final int value) throws IllegalStateException {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public IntStream filter(final IntPredicate predicate, final IntConsumer onDrop) throws IllegalStateException {
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
    public IntStream dropWhile(final IntPredicate predicate, final IntConsumer onDrop) throws IllegalStateException {
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
    public IntStream step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1) {
            return skip(0);
        }

        final long skip = step - 1;
        final IntIteratorEx iter = iteratorEx();

        final IntIterator intIterator = new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                final int next = iter.nextInt();
                iter.advance(skip);
                return next;
            }
        };

        return newStream(intIterator, isSorted());
    }

    @Override
    public IntStream scan(final IntBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new IntIteratorEx() { //NOSONAR
            private int res = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                if (isFirst) {
                    isFirst = false;
                    return (res = iter.nextInt());
                } else {
                    return (res = accumulator.applyAsInt(res, iter.nextInt()));
                }
            }
        }, false);
    }

    @Override
    public IntStream scan(final int init, final IntBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new IntIteratorEx() { //NOSONAR
            private int res = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                return (res = accumulator.applyAsInt(res, iter.nextInt()));
            }
        }, false);
    }

    @Override
    public IntStream scan(final int init, final boolean initIncluded, final IntBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (!initIncluded) {
            return scan(init, accumulator);
        }

        final IntIteratorEx iter = iteratorEx();

        return newStream(new IntIteratorEx() { //NOSONAR
            private boolean isFirst = true;
            private int res = init;

            @Override
            public boolean hasNext() {
                return isFirst || iter.hasNext();
            }

            @Override
            public int nextInt() {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (res = accumulator.applyAsInt(res, iter.nextInt()));
            }
        }, false);
    }

    @Override
    public IntStream top(final int n) throws IllegalStateException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return limit(0);
        }

        return top(n, INT_COMPARATOR);
    }

    @Override
    public IntStream intersection(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(multiset::remove).iteratorEx(), isSorted());
    }

    @Override
    public IntStream difference(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value)).iteratorEx(), isSorted());
    }

    @Override
    public IntStream symmetricDifference(final Collection<? extends Integer> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value)).append(Stream.of(c).filter(multiset::remove).mapToInt(UNBOX)).iteratorEx(),
                false);
    }

    @Override
    public IntStream reversed() throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private int[] elements;
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
            public int nextInt() {
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
            public int[] toArray() {
                if (!initialized) {
                    init();
                }

                final int[] a = new int[cursor - fromIndex];

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = elements[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<int[], Integer, Integer> tp = AbstractIntStream.this.arrayForIntermediateOp();

                    elements = tp._1;
                    fromIndex = tp._2;
                    toIndex = tp._3;

                    cursor = toIndex;
                }
            }
        }, false);
    }

    @Override
    public IntStream rotated(final int distance) throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private int[] elements;
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
            public int nextInt() {
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
            public int[] toArray() {
                if (!initialized) {
                    init();
                }

                final int[] a = new int[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = elements[((start + i) % len) + fromIndex];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<int[], Integer, Integer> tp = AbstractIntStream.this.arrayForIntermediateOp();

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
    public IntStream shuffled(final Random rnd) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(rnd, cs.rnd);

        return lazyLoad(a -> {
            N.shuffle(a, rnd);
            return a;
        }, false);
    }

    @Override
    public IntStream sorted() throws IllegalStateException {
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
    public IntStream reverseSorted() throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIteratorEx() { //NOSONAR
            private boolean initialized = false;
            private int[] sortedArray;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public int nextInt() {
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
            public int[] toArray() {
                if (!initialized) {
                    init();
                }

                final int[] a = new int[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = sortedArray[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    sortedArray = AbstractIntStream.this.toArrayForIntermediateOp();

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
     * Creates a lazily-loaded IntStream by applying the given array transformation operation.
     * The stream materializes all elements into an array and applies {@code op} when the returned
     * stream is first consumed.
     *
     * @param op the transformation to apply to the collected element array
     * @param sorted whether the resulting stream should be marked as sorted
     * @return a new IntStream backed by the transformed array
     */
    private IntStream lazyLoad(final UnaryOperator<int[]> op, final boolean sorted) {
        return IntStream.defer(() -> newStream(op.apply(toArrayForIntermediateOp()), sorted)).onClose(this::close);
    }

    @Override
    public IntStream cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIterator() { //NOSONAR
            private IntIterator iter = null;
            private IntList list = null;
            private int[] a = null;
            private int len = 0;
            private int cursor = -1;
            private int currentElement = 0;

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
            public int nextInt() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (len > 0) {
                    if (cursor >= len) {
                        cursor = 0;
                    }

                    return a[cursor++];
                } else {
                    currentElement = iter.nextInt();
                    list.add(currentElement);

                    return currentElement;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractIntStream.this.iteratorEx();
                    list = new IntList();
                }
            }
        }, false);
    }

    @Override
    public IntStream cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new IntIterator() { //NOSONAR
            private IntIterator iter = null;
            private IntList list = null;
            private int[] a = null;
            private int len = 0;
            private int cursor = -1;
            private int currentElement = 0;
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
                    // otherwise rounds >= 3 would have hasNext() return true while nextInt()
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
            public int nextInt() {
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
                    currentElement = iter.nextInt();
                    list.add(currentElement);

                    return currentElement;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractIntStream.this.iteratorEx();
                    list = new IntList();
                }
            }
        }, rounds <= 1 && isSorted());
    }

    @Override
    public Stream<IndexedInt> indexed() throws IllegalStateException {
        assertNotClosed();

        final MutableLong idx = MutableLong.of(0);

        //noinspection resource
        return newStream(sequential().mapToObj(t -> IndexedInt.of(t, idx.getAndIncrement())).iteratorEx(), true, INDEXED_INT_COMPARATOR);
    }

    @Override
    public Stream<Integer> boxed() throws IllegalStateException {
        assertNotClosed();

        return newStream(iteratorEx(), isSorted(), isSorted() ? INT_COMPARATOR : null);
    }

    @Override
    public final IntStream prepend(final int... a) throws IllegalStateException {
        assertNotClosed();

        return prepend(IntStream.of(a));
    }

    @Override
    public IntStream prepend(final IntStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return IntStream.concat(stream, this).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return IntStream.concat(stream, this);
        }
    }

    @Override
    public IntStream prepend(final OptionalInt op) throws IllegalStateException {
        assertNotClosed();

        // return prepend(op.stream());
        return op.isEmpty() ? this : prepend(op.orElseThrow());
    }

    @Override
    public final IntStream append(final int... a) throws IllegalStateException {
        assertNotClosed();

        return append(IntStream.of(a));
    }

    @Override
    public IntStream append(final IntStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return IntStream.concat(this, stream).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return IntStream.concat(this, stream);
        }
    }

    @Override
    public IntStream append(final OptionalInt op) { //NOSONAR
        assertNotClosed();

        // return append(op.stream());
        return op.isEmpty() ? this : append(op.orElseThrow());
    }

    @Override
    public final IntStream appendIfEmpty(final int... a) throws IllegalStateException {
        assertNotClosed();

        return appendIfEmpty(() -> IntStream.of(a));
    }

    @Override
    public IntStream mergeWith(final IntStream b, final IntBiFunction<MergeResult> nextSelector) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return IntStream.merge(this, b, nextSelector).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return IntStream.merge(this, b, nextSelector);
        }
    }

    @Override
    public IntStream zipWith(final IntStream b, final IntBinaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return IntStream.zip(this, b, zipFunction);
    }

    @Override
    public IntStream zipWith(final IntStream b, final IntStream c, final IntTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return IntStream.zip(this, b, c, zipFunction);
    }

    @Override
    public IntStream zipWith(final IntStream b, final int valueForNoneA, final int valueForNoneB, final IntBinaryOperator zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        return IntStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public IntStream zipWith(final IntStream b, final IntStream c, final int valueForNoneA, final int valueForNoneB, final int valueForNoneC,
            final IntTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return IntStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.IntFunction<? extends K, E> keyMapper,
            final Throwables.IntFunction<? extends V, E2> valueMapper) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.IntFunction<? extends K, E> keyMapper,
            final Throwables.IntFunction<? extends V, E2> valueMapper, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Fn.throwingMerger(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.IntFunction<? extends K, E> keyMapper,
            final Throwables.IntFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    @Override
    public <K, D, E extends Exception> Map<K, D> groupTo(final Throwables.IntFunction<? extends K, E> keyMapper,
            final Collector<? super Integer, ?, D> downstream) throws E {
        assertNotClosed();

        return groupTo(keyMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <E extends Exception> void forEachIndexed(final Throwables.IntIntConsumer<E> action) throws IllegalStateException, E {
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
    public OptionalInt first() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final IntIterator iter = iteratorEx();

            return iter.hasNext() ? OptionalInt.of(iter.nextInt()) : OptionalInt.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt last() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final IntIterator iter = iteratorEx();

            if (!iter.hasNext()) {
                return OptionalInt.empty();
            }

            int next = iter.nextInt();

            while (iter.hasNext()) {
                next = iter.nextInt();
            }

            return OptionalInt.of(next);
        } finally {
            close();
        }
    }

    @SuppressWarnings("DuplicateThrows")
    @Override
    public OptionalInt onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final IntIterator iter = iteratorEx();

            final OptionalInt result = iter.hasNext() ? OptionalInt.of(iter.nextInt()) : OptionalInt.empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new TooManyElementsException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.nextInt()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalInt findAny(final Throwables.IntPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        return findFirst(predicate);
    }

    @Override
    public Optional<Map<Percentage, Integer>> percentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final int[] a = sorted().toArray();

            if (a.length == 0) {
                return Optional.empty();
            }

            return Optional.of(N.percentilesOfSorted(a));
        } finally {
            close();
        }
    }

    @Override
    public Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> summaryStatisticsAndPercentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final int[] a = sorted().toArray();

            if (N.isEmpty(a)) {
                return Pair.of(new IntSummaryStatistics(), Optional.empty());
            } else {
                // Compute the sum as a long locally; StreamBase.sum(int[]) wraps the result in
                // toIntExact and throws ArithmeticException on overflow, but IntSummaryStatistics's
                // 4th argument is a long and the analogous summaryStatistics() path accumulates
                // into a long without overflow. Diverging behavior here would cause one API to
                // throw on the same data the other handles cleanly.
                long s = 0;
                for (int v : a) {
                    s += v;
                }
                return Pair.of(new IntSummaryStatistics(a.length, a[0], a[a.length - 1], s), Optional.of(N.percentilesOfSorted(a)));
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
            final IntIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextInt());
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
            final IntIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextInt());
            }
        } finally {
            close();
        }

        return joiner;
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjIntConsumer<? super R> accumulator) throws IllegalStateException {
        assertNotClosed();

        @SuppressWarnings("UnnecessaryLocalVariable")
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }

    @Override
    public IntIterator iterator() throws IllegalStateException {
        assertNotClosed();

        if (!isEmptyCloseHandlers(closeHandlers()) && logger.isWarnEnabled()) {
            logger.warn("Remember to close {} after iteration because it has close handlers", ClassUtil.getSimpleClassName(getClass()));
        }

        return iteratorEx();
    }
}

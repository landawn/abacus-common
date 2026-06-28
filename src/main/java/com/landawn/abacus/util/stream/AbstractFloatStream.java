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
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IndexedFloat;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.KahanSummation;
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
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.FloatBiPredicate;
import com.landawn.abacus.util.function.FloatBinaryOperator;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatFunction;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatTernaryOperator;
import com.landawn.abacus.util.function.FloatTriPredicate;
import com.landawn.abacus.util.function.ObjFloatConsumer;
import com.landawn.abacus.util.function.ToFloatFunction;

/**
 * Abstract base implementation of FloatStream providing common functionality for float stream operations.
 * This class serves as the foundation for concrete float stream implementations, offering intermediate
 * and terminal operations on sequences of primitive float values.
 *
 * <p>This implementation includes:
 * <ul>
 * <li>Rate limiting and delay operations for controlling stream processing speed</li>
 * <li>Filtering, mapping, and transformation operations</li>
 * <li>Collection and aggregation operations with precision handling (using Kahan summation)</li>
 * <li>Sorting, reversing, and rotation operations</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Subclasses implement concrete stream behavior
 * FloatStream stream = new ArrayFloatStream(new float[] {1.0f, 2.0f, 3.0f});
 * stream.filter(f -> f > 1.5f).forEach(System.out::println);
 * }</pre>
 *
 */
abstract class AbstractFloatStream extends FloatStream {

    /**
     * Constructs an AbstractFloatStream with the specified sorting state and close handlers.
     *
     * @param sorted whether the stream elements are in sorted order
     * @param closeHandlers collection of handlers to execute when the stream is closed
     */
    AbstractFloatStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);
    }

    @Override
    public FloatStream rateLimited(final RateLimiter rateLimiter) throws IllegalArgumentException {
        checkArgNotNull(rateLimiter, cs.rateLimiter);

        final FloatConsumer action = it -> rateLimiter.acquire();

        if (isParallel()) {
            //noinspection resource
            return sequential().onEach(action).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return onEach(action);
        }
    }

    @Override
    public FloatStream delay(final Duration delay) throws IllegalArgumentException {
        checkArgNotNull(delay, cs.delay);

        final long millis = delay.toMillis();

        final FloatConsumer action = new FloatConsumer() {
            private boolean isFirst = true;

            @Override
            public void accept(final float it) {
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
    public FloatStream debounce(Duration duration) {
        checkArgNotNull(duration, cs.duration);
        checkArgPositive(duration.toMillis(), cs.duration);

        if (isParallel()) {
            return psp(s -> s.debounce(duration));
        }

        final FloatIteratorEx iter = iteratorEx();

        return newStream(new FloatIteratorEx() {
            private final long durationMillis = duration.toMillis();
            private float prev = 0; // the most recent element of the current burst, awaiting a quiet gap
            private boolean hasPrev = false;
            private long prevTime = 0;
            private float next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                if (hasNext) {
                    return true;
                }

                while (iter.hasNext()) {
                    final float val = iter.nextFloat();
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
            public float nextFloat() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;
                return next;
            }
        }, isSorted());
    }

    @Override
    public FloatStream skipUntil(final FloatPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return dropWhile(t -> !predicate.test(t));
    }

    @Override
    public FloatStream distinct() throws IllegalStateException {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        //noinspection resource
        return newStream(sequential().filter(set::add).iteratorEx(), isSorted());
    }

    @Override
    public FloatStream flatMapArray(final FloatFunction<float[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMap(t -> FloatStream.of(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatmapToObj(final FloatFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatMapArrayToObj(final FloatFunction<T[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public FloatStream mapPartial(final FloatFunction<OptionalFloat> mapper) {
        if (isParallel()) {
            //noinspection resource
            return mapToObj(mapper).psp(s -> s.filter(Fn.IS_PRESENT_FLOAT).mapToFloat(Fn.GET_AS_FLOAT));
        } else {
            //noinspection resource
            return mapToObj(mapper).filter(Fn.IS_PRESENT_FLOAT).mapToFloat(Fn.GET_AS_FLOAT);
        }
    }

    @Override
    public FloatStream rangeMap(final FloatBiPredicate sameRange, final FloatBinaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        final FloatIteratorEx iter = iteratorEx();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private float left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public float nextFloat() {
                left = hasNext ? next : iter.nextFloat();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextFloat();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.applyAsFloat(left, right);
            }
        }, false);
    }

    @Override
    public <T> Stream<T> rangeMapToObj(final FloatBiPredicate sameRange, final FloatBiFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        final FloatIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private float left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                left = hasNext ? next : iter.nextFloat();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextFloat();

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
    public Stream<FloatList> collapse(final FloatBiPredicate collapsible) throws IllegalStateException {
        assertNotClosed();

        final FloatIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private float next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public FloatList next() {
                final FloatList result = new FloatList(9);
                result.add(hasNext ? next : (next = iter.nextFloat()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextFloat()))) {
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
    public FloatStream collapse(final FloatBiPredicate collapsible, final FloatBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final FloatIteratorEx iter = iteratorEx();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private float next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public float nextFloat() {
                float res = hasNext ? next : (next = iter.nextFloat());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextFloat()))) {
                        res = mergeFunction.applyAsFloat(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public FloatStream collapse(final FloatTriPredicate collapsible, final FloatBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final FloatIteratorEx iter = iteratorEx();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private float next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public float nextFloat() {
                final float first = hasNext ? next : (next = iter.nextFloat());
                float res = first;

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.nextFloat()))) {
                        res = mergeFunction.applyAsFloat(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public FloatStream skip(final long n, final FloatConsumer action) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);
        checkArgNotNull(action, cs.action);

        if (n == 0) {
            return this;
        }

        final FloatPredicate filter = isParallel() ? new FloatPredicate() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(final float value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new FloatPredicate() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(final float value) throws IllegalStateException {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public FloatStream filter(final FloatPredicate predicate, final FloatConsumer onDrop) throws IllegalStateException {
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
    public FloatStream dropWhile(final FloatPredicate predicate, final FloatConsumer onDrop) throws IllegalStateException {
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
    public FloatStream step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1) {
            return skip(0);
        }

        final long skip = step - 1;
        final FloatIteratorEx iter = iteratorEx();

        final FloatIterator floatIterator = new FloatIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                final float next = iter.nextFloat();
                iter.advance(skip);
                return next;
            }
        };

        return newStream(floatIterator, isSorted());
    }

    @Override
    public FloatStream scan(final FloatBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final FloatIteratorEx iter = iteratorEx();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private float res = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                if (isFirst) {
                    isFirst = false;
                    return (res = iter.nextFloat());
                } else {
                    return (res = accumulator.applyAsFloat(res, iter.nextFloat()));
                }
            }
        }, false);
    }

    @Override
    public FloatStream scan(final float init, final FloatBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final FloatIteratorEx iter = iteratorEx();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private float res = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                return (res = accumulator.applyAsFloat(res, iter.nextFloat()));
            }
        }, false);
    }

    @Override
    public FloatStream scan(final float init, final boolean initIncluded, final FloatBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (!initIncluded) {
            return scan(init, accumulator);
        }

        final FloatIteratorEx iter = iteratorEx();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private boolean isFirst = true;
            private float res = init;

            @Override
            public boolean hasNext() {
                return isFirst || iter.hasNext();
            }

            @Override
            public float nextFloat() {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (res = accumulator.applyAsFloat(res, iter.nextFloat()));
            }
        }, false);
    }

    @Override
    public FloatStream top(final int n) throws IllegalStateException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return limit(0);
        }

        return top(n, FLOAT_COMPARATOR);
    }

    @Override
    public FloatStream intersection(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(multiset::remove).iteratorEx(), isSorted());
    }

    @Override
    public FloatStream difference(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value)).iteratorEx(), isSorted());
    }

    @Override
    public FloatStream symmetricDifference(final Collection<? extends Float> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value))
                .append(Stream.of(c).filter(multiset::remove).mapToFloat(ToFloatFunction.UNBOX))
                .iteratorEx(), false);
    }

    @Override
    public FloatStream reversed() throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private float[] elements;
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
            public float nextFloat() {
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
            public float[] toArray() {
                if (!initialized) {
                    init();
                }

                final float[] a = new float[cursor - fromIndex];

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = elements[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<float[], Integer, Integer> tp = AbstractFloatStream.this.arrayForIntermediateOp();

                    elements = tp._1;
                    fromIndex = tp._2;
                    toIndex = tp._3;

                    cursor = toIndex;
                }
            }
        }, false);
    }

    @Override
    public FloatStream rotated(final int distance) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private float[] elements;
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
            public float nextFloat() {
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
            public float[] toArray() {
                if (!initialized) {
                    init();
                }

                final float[] a = new float[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = elements[((start + i) % len) + fromIndex];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<float[], Integer, Integer> tp = AbstractFloatStream.this.arrayForIntermediateOp();

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
    public FloatStream shuffled(final Random rnd) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(rnd, cs.rnd);

        return lazyLoad(a -> {
            N.shuffle(a, rnd);
            return a;
        }, false);
    }

    @Override
    public FloatStream sorted() throws IllegalStateException {
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
    public FloatStream reverseSorted() throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private boolean initialized = false;
            private float[] aar;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public float nextFloat() {
                if (!initialized) {
                    init();
                }

                if (cursor <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return aar[--cursor];
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
            public float[] toArray() {
                if (!initialized) {
                    init();
                }

                final float[] a = new float[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = aar[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    aar = AbstractFloatStream.this.toArrayForIntermediateOp();

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

    /**
     * Creates a lazily-loaded FloatStream by applying the given array transformation operation.
     * The stream materializes all elements into an array and applies {@code op} when the returned
     * stream is first consumed.
     *
     * @param op the transformation to apply to the collected element array
     * @param sorted whether the resulting stream should be marked as sorted
     * @return a new FloatStream backed by the transformed array
     */
    private FloatStream lazyLoad(final UnaryOperator<float[]> op, final boolean sorted) {
        return FloatStream.defer(() -> newStream(op.apply(toArrayForIntermediateOp()), sorted)).onClose(this::close);
    }

    @Override
    public FloatStream cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIterator() { //NOSONAR
            private FloatIterator iter = null;
            private FloatList list = null;
            private float[] a = null;
            private int len = 0;
            private int cursor = -1;
            private float currentElement = 0;

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
            public float nextFloat() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (len > 0) {
                    if (cursor >= len) {
                        cursor = 0;
                    }

                    return a[cursor++];
                } else {
                    currentElement = iter.nextFloat();
                    list.add(currentElement);

                    return currentElement;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractFloatStream.this.iteratorEx();
                    list = new FloatList();
                }
            }
        }, false);
    }

    @Override
    public FloatStream cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new FloatIterator() { //NOSONAR
            private FloatIterator iter = null;
            private FloatList list = null;
            private float[] a = null;
            private int len = 0;
            private int cursor = -1;
            private float currentElement = 0;
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
                    // otherwise rounds >= 3 would have hasNext() return true while nextFloat()
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
            public float nextFloat() {
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
                    currentElement = iter.nextFloat();
                    list.add(currentElement);

                    return currentElement;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractFloatStream.this.iteratorEx();
                    list = new FloatList();
                }
            }
        }, rounds <= 1 && isSorted());
    }

    @Override
    public Stream<IndexedFloat> indexed() throws IllegalStateException {
        assertNotClosed();

        final MutableLong idx = MutableLong.of(0);

        //noinspection resource
        return newStream(sequential().mapToObj(t -> IndexedFloat.of(t, idx.getAndIncrement())).iteratorEx(), true, INDEXED_FLOAT_COMPARATOR);
    }

    @Override
    public Stream<Float> boxed() throws IllegalStateException {
        assertNotClosed();

        return newStream(iteratorEx(), isSorted(), isSorted() ? FLOAT_COMPARATOR : null);
    }

    @Override
    public final FloatStream prepend(final float... a) throws IllegalStateException {
        assertNotClosed();

        return prepend(FloatStream.of(a));
    }

    @Override
    public FloatStream prepend(final FloatStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return FloatStream.concat(stream, this).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return FloatStream.concat(stream, this);
        }
    }

    @Override
    public FloatStream prepend(final OptionalFloat op) throws IllegalStateException {
        assertNotClosed();

        // return prepend(op.stream());
        return op.isEmpty() ? this : prepend(op.orElseThrow());
    }

    @Override
    public final FloatStream append(final float... a) throws IllegalStateException {
        assertNotClosed();

        return append(FloatStream.of(a));
    }

    @Override
    public FloatStream append(final FloatStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return FloatStream.concat(this, stream).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return FloatStream.concat(this, stream);
        }
    }

    @Override
    public FloatStream append(final OptionalFloat op) { //NOSONAR
        assertNotClosed();

        // return append(op.stream());
        return op.isEmpty() ? this : append(op.orElseThrow());
    }

    @Override
    public final FloatStream appendIfEmpty(final float... a) throws IllegalStateException {
        assertNotClosed();

        return appendIfEmpty(() -> FloatStream.of(a));
    }

    @Override
    public FloatStream mergeWith(final FloatStream b, final FloatBiFunction<MergeResult> nextSelector) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return FloatStream.merge(this, b, nextSelector).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return FloatStream.merge(this, b, nextSelector);
        }
    }

    @Override
    public FloatStream zipWith(final FloatStream b, final FloatBinaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return FloatStream.zip(this, b, zipFunction);
    }

    @Override
    public FloatStream zipWith(final FloatStream b, final FloatStream c, final FloatTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return FloatStream.zip(this, b, c, zipFunction);
    }

    @Override
    public FloatStream zipWith(final FloatStream b, final float valueForNoneA, final float valueForNoneB, final FloatBinaryOperator zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        return FloatStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public FloatStream zipWith(final FloatStream b, final FloatStream c, final float valueForNoneA, final float valueForNoneB, final float valueForNoneC,
            final FloatTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return FloatStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.FloatFunction<? extends K, E> keyMapper,
            final Throwables.FloatFunction<? extends V, E2> valueMapper) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.FloatFunction<? extends K, E> keyMapper,
            final Throwables.FloatFunction<? extends V, E2> valueMapper, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Fn.throwingMerger(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.FloatFunction<? extends K, E> keyMapper,
            final Throwables.FloatFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    @Override
    public <K, D, E extends Exception> Map<K, D> groupTo(final Throwables.FloatFunction<? extends K, E> keyMapper,
            final Collector<? super Float, ?, D> downstream) throws E {
        assertNotClosed();

        return groupTo(keyMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <E extends Exception> void forEachIndexed(final Throwables.IntFloatConsumer<E> action) throws IllegalStateException, E {
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
    public double sum() throws IllegalStateException {
        assertNotClosed();

        try {
            return summation().sum();
        } finally {
            close();
        }
    }

    /**
     * Accumulates all elements of this stream into a {@link KahanSummation} for
     * numerically-stable sum and average computation.
     * The stream's elements are fully drained by this call; callers are responsible for closing the stream.
     *
     * @return a {@link KahanSummation} containing the accumulated values
     */
    private KahanSummation summation() {
        final KahanSummation summation = new KahanSummation();

        // not thread safe
        // final FloatConsumer action = summation::add;
        // this.forEach(action);

        final FloatIterator iter = iteratorEx();

        while (iter.hasNext()) {
            summation.add(iter.nextFloat());
        }

        return summation;
    }

    @Override
    public OptionalDouble average() throws IllegalStateException {
        assertNotClosed();

        try {
            return summation().average();
        } finally {
            close();
        }
    }

    @Override
    public OptionalFloat first() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final FloatIterator iter = iteratorEx();

            return iter.hasNext() ? OptionalFloat.of(iter.nextFloat()) : OptionalFloat.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalFloat last() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final FloatIterator iter = iteratorEx();

            if (!iter.hasNext()) {
                return OptionalFloat.empty();
            }

            float next = iter.nextFloat();

            while (iter.hasNext()) {
                next = iter.nextFloat();
            }

            return OptionalFloat.of(next);
        } finally {
            close();
        }
    }

    @SuppressWarnings("DuplicateThrows")
    @Override
    public OptionalFloat onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final FloatIterator iter = iteratorEx();

            final OptionalFloat result = iter.hasNext() ? OptionalFloat.of(iter.nextFloat()) : OptionalFloat.empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new TooManyElementsException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.nextFloat()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalFloat findAny(final Throwables.FloatPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        return findFirst(predicate);
    }

    @Override
    public Optional<Map<Percentage, Float>> percentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final float[] a = sorted().toArray();

            if (a.length == 0) {
                return Optional.empty();
            }

            return Optional.of(N.percentilesOfSorted(a));
        } finally {
            close();
        }
    }

    @Override
    public Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> summaryStatisticsAndPercentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final float[] a = sorted().toArray();

            if (N.isEmpty(a)) {
                return Pair.of(new FloatSummaryStatistics(), Optional.empty());
            } else {
                // For NaN-aware semantics consistent with FloatSummaryStatistics.accept
                // (which uses Math.min/Math.max so any NaN propagates to both min and max),
                // detect NaN in the sorted array. With Float.compare ordering used by sort,
                // After sorting, NaN sorts to the end; guard both ends defensively.
                final float min;
                final float max;
                if (Float.isNaN(a[0]) || Float.isNaN(a[a.length - 1])) {
                    min = Float.NaN;
                    max = Float.NaN;
                } else {
                    min = a[0];
                    max = a[a.length - 1];
                }
                return Pair.of(new FloatSummaryStatistics(a.length, min, max, sum(a)), Optional.of(N.percentilesOfSorted(a)));
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
            final FloatIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextFloat());
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
            final FloatIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextFloat());
            }
        } finally {
            close();
        }

        return joiner;
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjFloatConsumer<? super R> accumulator) throws IllegalStateException {
        assertNotClosed();

        @SuppressWarnings("UnnecessaryLocalVariable")
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }

    @Override
    public FloatIterator iterator() throws IllegalStateException {
        assertNotClosed();

        if (!isEmptyCloseHandlers(closeHandlers()) && logger.isWarnEnabled()) {
            logger.warn("Remember to close {} after iteration because it has close handlers", ClassUtil.getSimpleClassName(getClass()));
        }

        return iteratorEx();
    }
}

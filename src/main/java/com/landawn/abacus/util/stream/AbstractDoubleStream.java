/*
 * Copyright (C) 2016 HaiYang Li
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

import static com.landawn.abacus.util.function.ToDoubleFunction.UNBOX;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IndexedDouble;
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
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleBiPredicate;
import com.landawn.abacus.util.function.DoubleMapMultiConsumer;
import com.landawn.abacus.util.function.DoubleTernaryOperator;
import com.landawn.abacus.util.function.DoubleTriPredicate;

/**
 * Abstract base implementation of DoubleStream providing common functionality for double stream operations.
 * This class serves as the foundation for concrete double stream implementations, offering intermediate
 * and terminal operations on sequences of primitive double values.
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
 * DoubleStream stream = new ArrayDoubleStream(new double[] {1.0, 2.0, 3.0});
 * stream.filter(d -> d > 1.5).forEach(System.out::println);
 * }</pre>
 *
 */
abstract class AbstractDoubleStream extends DoubleStream {

    /**
     * Constructs an AbstractDoubleStream with the specified sorting state and close handlers.
     *
     * @param sorted whether the stream elements are in sorted order
     * @param closeHandlers collection of handlers to execute when the stream is closed
     */
    AbstractDoubleStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);
    }

    @Override
    public DoubleStream rateLimited(final RateLimiter rateLimiter) throws IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(rateLimiter, cs.rateLimiter);

        final DoubleConsumer action = it -> rateLimiter.acquire();

        if (isParallel()) {
            //noinspection resource
            return sequential().onEach(action).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return onEach(action);
        }
    }

    @Override
    public DoubleStream delay(final Duration delay) throws IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(delay, cs.delay);

        final long millis = delay.toMillis();

        final DoubleConsumer action = new DoubleConsumer() {
            private boolean isFirst = true;

            @Override
            public void accept(final double it) {
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
    public DoubleStream debounce(Duration duration) throws IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(duration, cs.duration);
        checkArgPositive(duration.toMillis(), cs.duration);

        if (isParallel()) {
            return psp(s -> s.debounce(duration));
        }

        final DoubleIteratorEx iter = iteratorEx();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private final long durationMillis = duration.toMillis();
            private double prev = 0; // the most recent element of the current burst, awaiting a quiet gap
            private boolean hasPrev = false;
            private long prevTime = 0;
            private double next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                if (hasNext) {
                    return true;
                }

                while (iter.hasNext()) {
                    final double val = iter.nextDouble();
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
            public double nextDouble() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;
                return next;
            }
        }, isSorted());
    }

    @Override
    public DoubleStream skipUntil(final DoublePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return dropWhile(t -> !predicate.test(t));
    }

    @Override
    public DoubleStream distinct() throws IllegalStateException {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        //noinspection resource
        return newStream(sequential().filter(set::add).iteratorEx(), isSorted());
    }

    @Override
    public DoubleStream flatMapArray(final DoubleFunction<double[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMap(t -> DoubleStream.of(mapper.apply(t)));
    }

    @Override
    public DoubleStream flattMap(final DoubleFunction<? extends java.util.stream.DoubleStream> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMap(t -> DoubleStream.from(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatmapToObj(final DoubleFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatMapArrayToObj(final DoubleFunction<T[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public DoubleStream mapMulti(final DoubleMapMultiConsumer mapper) {
        assertNotClosed();

        final DoubleFunction<DoubleStream> secondMapper = t -> {
            final SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble();

            mapper.accept(t, buffer);

            return DoubleStream.of(buffer.iterator());
        };

        return flatMap(secondMapper);
    }

    @Override
    public DoubleStream mapPartial(final DoubleFunction<OptionalDouble> mapper) {
        assertNotClosed();

        if (isParallel()) {
            //noinspection resource
            return mapToObj(mapper).psp(s -> s.filter(Fn.IS_PRESENT_DOUBLE).mapToDouble(Fn.GET_AS_DOUBLE));
        } else {
            //noinspection resource
            return mapToObj(mapper).filter(Fn.IS_PRESENT_DOUBLE).mapToDouble(Fn.GET_AS_DOUBLE);
        }
    }

    @Override
    public DoubleStream mapPartialJdk(final DoubleFunction<java.util.OptionalDouble> mapper) {
        assertNotClosed();

        if (isParallel()) {
            //noinspection resource
            return mapToObj(mapper).psp(s -> s.filter(Fn.IS_PRESENT_DOUBLE_JDK).mapToDouble(Fn.GET_AS_DOUBLE_JDK));
        } else {
            //noinspection resource
            return mapToObj(mapper).filter(Fn.IS_PRESENT_DOUBLE_JDK).mapToDouble(Fn.GET_AS_DOUBLE_JDK);
        }
    }

    @Override
    public DoubleStream rangeMap(final DoubleBiPredicate sameRange, final DoubleBinaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        final DoubleIteratorEx iter = iteratorEx();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private double left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public double nextDouble() {
                left = hasNext ? next : iter.nextDouble();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextDouble();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.applyAsDouble(left, right);
            }
        }, false);
    }

    @Override
    public <T> Stream<T> rangeMapToObj(final DoubleBiPredicate sameRange, final DoubleBiFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        final DoubleIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private double left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                left = hasNext ? next : iter.nextDouble();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextDouble();

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
    public Stream<DoubleList> collapse(final DoubleBiPredicate collapsible) throws IllegalStateException {
        assertNotClosed();

        final DoubleIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private double next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public DoubleList next() {
                final DoubleList result = new DoubleList(9);
                result.add(hasNext ? next : (next = iter.nextDouble()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextDouble()))) {
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
    public DoubleStream collapse(final DoubleBiPredicate collapsible, final DoubleBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final DoubleIteratorEx iter = iteratorEx();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private double next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public double nextDouble() {
                double res = hasNext ? next : (next = iter.nextDouble());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextDouble()))) {
                        res = mergeFunction.applyAsDouble(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public DoubleStream collapse(final DoubleTriPredicate collapsible, final DoubleBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final DoubleIteratorEx iter = iteratorEx();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private double next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public double nextDouble() {
                final double first = hasNext ? next : (next = iter.nextDouble());
                double res = first;

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.nextDouble()))) {
                        res = mergeFunction.applyAsDouble(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public DoubleStream skip(final long n, final DoubleConsumer action) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);
        checkArgNotNull(action, cs.action);

        if (n == 0) {
            return this;
        }

        final DoublePredicate filter = isParallel() ? new DoublePredicate() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(final double value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new DoublePredicate() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(final double value) throws IllegalStateException {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public DoubleStream filter(final DoublePredicate predicate, final DoubleConsumer onDrop) throws IllegalStateException {
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
    public DoubleStream dropWhile(final DoublePredicate predicate, final DoubleConsumer onDrop) throws IllegalStateException {
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
    public DoubleStream step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1) {
            return skip(0);
        }

        final long skip = step - 1;
        final DoubleIteratorEx iter = iteratorEx();

        final DoubleIterator doubleIterator = new DoubleIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public double nextDouble() {
                final double next = iter.nextDouble();
                iter.advance(skip);
                return next;
            }
        };

        return newStream(doubleIterator, isSorted());
    }

    @Override
    public DoubleStream scan(final DoubleBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final DoubleIteratorEx iter = iteratorEx();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private double res = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public double nextDouble() {
                if (isFirst) {
                    isFirst = false;
                    return (res = iter.nextDouble());
                } else {
                    return (res = accumulator.applyAsDouble(res, iter.nextDouble()));
                }
            }
        }, false);
    }

    @Override
    public DoubleStream scan(final double init, final DoubleBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final DoubleIteratorEx iter = iteratorEx();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private double res = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public double nextDouble() {
                return (res = accumulator.applyAsDouble(res, iter.nextDouble()));
            }
        }, false);
    }

    @Override
    public DoubleStream scan(final double init, final boolean initIncluded, final DoubleBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (!initIncluded) {
            return scan(init, accumulator);
        }

        final DoubleIteratorEx iter = iteratorEx();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private boolean isFirst = true;
            private double res = init;

            @Override
            public boolean hasNext() {
                return isFirst || iter.hasNext();
            }

            @Override
            public double nextDouble() {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (res = accumulator.applyAsDouble(res, iter.nextDouble()));
            }
        }, false);
    }

    @Override
    public DoubleStream intersection(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(multiset::remove).iteratorEx(), isSorted());
    }

    @Override
    public DoubleStream difference(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value)).iteratorEx(), isSorted());
    }

    @Override
    public DoubleStream symmetricDifference(final Collection<? extends Double> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value)).append(Stream.of(c).filter(multiset::remove).mapToDouble(UNBOX)).iteratorEx(),
                false);
    }

    @Override
    public DoubleStream reversed() throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private double[] elements;
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
            public double nextDouble() {
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
            public double[] toArray() {
                if (!initialized) {
                    init();
                }

                final double[] a = new double[cursor - fromIndex];

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = elements[cursor - i - 1];
                }

                cursor = fromIndex;

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<double[], Integer, Integer> tp = AbstractDoubleStream.this.arrayForIntermediateOp();

                    elements = tp._1;
                    fromIndex = tp._2;
                    toIndex = tp._3;

                    cursor = toIndex;
                }
            }
        }, false);
    }

    @Override
    public DoubleStream rotated(final int distance) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private double[] elements;
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
            public double nextDouble() {
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
            public double[] toArray() {
                if (!initialized) {
                    init();
                }

                final double[] a = new double[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = elements[((start + i) % len) + fromIndex];
                }

                cnt = len;

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<double[], Integer, Integer> tp = AbstractDoubleStream.this.arrayForIntermediateOp();

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
    public DoubleStream shuffled(final Random rnd) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(rnd, cs.rnd);

        return lazyLoad(a -> {
            N.shuffle(a, rnd);
            return a;
        }, false);
    }

    @Override
    public DoubleStream sorted() throws IllegalStateException {
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
    public DoubleStream reverseSorted() throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private boolean initialized = false;
            private double[] aar;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public double nextDouble() {
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
            public double[] toArray() {
                if (!initialized) {
                    init();
                }

                final double[] a = new double[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = aar[cursor - i - 1];
                }

                cursor = 0;

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    aar = AbstractDoubleStream.this.toArrayForIntermediateOp();

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
     * Creates a lazily-loaded DoubleStream by applying the given array transformation operation.
     * The stream materializes all elements into an array and applies {@code op} when the returned
     * stream is first consumed.
     *
     * @param op the transformation to apply to the collected element array
     * @param sorted whether the resulting stream should be marked as sorted
     * @return a new DoubleStream backed by the transformed array
     */
    private DoubleStream lazyLoad(final UnaryOperator<double[]> op, final boolean sorted) {
        return DoubleStream.defer(() -> newStream(op.apply(toArrayForIntermediateOp()), sorted)).onClose(this::close);
    }

    @Override
    public DoubleStream cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIterator() { //NOSONAR
            private DoubleIterator iter = null;
            private DoubleList list = null;
            private double[] a = null;
            private int len = 0;
            private int cursor = -1;
            private double currentElement = 0;

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
            public double nextDouble() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (len > 0) {
                    if (cursor >= len) {
                        cursor = 0;
                    }

                    return a[cursor++];
                } else {
                    currentElement = iter.nextDouble();
                    list.add(currentElement);

                    return currentElement;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractDoubleStream.this.iteratorEx();
                    list = new DoubleList();
                }
            }
        }, false);
    }

    @Override
    public DoubleStream cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new DoubleIterator() { //NOSONAR
            private DoubleIterator iter = null;
            private DoubleList list = null;
            private double[] a = null;
            private int len = 0;
            private int cursor = -1;
            private double currentElement = 0;
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
                    // otherwise rounds >= 3 would have hasNext() return true while nextDouble()
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
            public double nextDouble() {
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
                    currentElement = iter.nextDouble();
                    list.add(currentElement);

                    return currentElement;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractDoubleStream.this.iteratorEx();
                    list = new DoubleList();
                }
            }
        }, rounds <= 1 && isSorted());
    }

    @Override
    public Stream<IndexedDouble> indexed() throws IllegalStateException {
        assertNotClosed();

        final MutableLong idx = MutableLong.of(0);

        //noinspection resource
        return newStream(sequential().mapToObj(t -> IndexedDouble.of(t, idx.getAndIncrement())).iteratorEx(), true, INDEXED_DOUBLE_COMPARATOR);
    }

    @Override
    public Stream<Double> boxed() throws IllegalStateException {
        assertNotClosed();

        return newStream(iteratorEx(), isSorted(), isSorted() ? DOUBLE_COMPARATOR : null);
    }

    @Override
    public final DoubleStream prepend(final double... a) throws IllegalStateException {
        assertNotClosed();

        return prepend(DoubleStream.of(a));
    }

    @Override
    public DoubleStream prepend(final DoubleStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return DoubleStream.concat(stream, this).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return DoubleStream.concat(stream, this);
        }
    }

    @Override
    public DoubleStream prepend(final OptionalDouble op) throws IllegalStateException {
        assertNotClosed();

        // return prepend(op.stream());
        return op.isEmpty() ? this : prepend(op.orElseThrow());
    }

    @Override
    public final DoubleStream append(final double... a) throws IllegalStateException {
        assertNotClosed();

        return append(DoubleStream.of(a));
    }

    @Override
    public DoubleStream append(final DoubleStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return DoubleStream.concat(this, stream).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return DoubleStream.concat(this, stream);
        }
    }

    @Override
    public DoubleStream append(final OptionalDouble op) { //NOSONAR
        assertNotClosed();

        // return append(op.stream());
        return op.isEmpty() ? this : append(op.orElseThrow());
    }

    @Override
    public final DoubleStream appendIfEmpty(final double... a) throws IllegalStateException {
        assertNotClosed();

        return appendIfEmpty(() -> DoubleStream.of(a));
    }

    @Override
    public DoubleStream mergeWith(final DoubleStream b, final DoubleBiFunction<MergeResult> nextSelector) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return DoubleStream.merge(this, b, nextSelector).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return DoubleStream.merge(this, b, nextSelector);
        }
    }

    @Override
    public DoubleStream zipWith(final DoubleStream b, final DoubleBinaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return DoubleStream.zip(this, b, zipFunction);
    }

    @Override
    public DoubleStream zipWith(final DoubleStream b, final DoubleStream c, final DoubleTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return DoubleStream.zip(this, b, c, zipFunction);
    }

    @Override
    public DoubleStream zipWith(final DoubleStream b, final double valueForNoneA, final double valueForNoneB, final DoubleBinaryOperator zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        return DoubleStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public DoubleStream zipWith(final DoubleStream b, final DoubleStream c, final double valueForNoneA, final double valueForNoneB, final double valueForNoneC,
            final DoubleTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return DoubleStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    @Override
    public DoubleStream top(final int n) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return limit(0);
        }

        return top(n, DOUBLE_COMPARATOR);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.DoubleFunction<? extends K, E> keyMapper,
            final Throwables.DoubleFunction<? extends V, E2> valueMapper) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.DoubleFunction<? extends K, E> keyMapper,
            final Throwables.DoubleFunction<? extends V, E2> valueMapper, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Fn.throwingMerger(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.DoubleFunction<? extends K, E> keyMapper,
            final Throwables.DoubleFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    @Override
    public <K, D, E extends Exception> Map<K, D> groupTo(final Throwables.DoubleFunction<? extends K, E> keyMapper,
            final Collector<? super Double, ?, D> downstream) throws IllegalStateException, E {
        assertNotClosed();

        return groupTo(keyMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <E extends Exception> void forEachIndexed(final Throwables.IntDoubleConsumer<E> action) throws IllegalStateException, E {
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
        // final com.landawn.abacus.util.function.DoubleConsumer action = summation::add;
        // this.forEach(action);

        final DoubleIteratorEx iter = iteratorEx();

        while (iter.hasNext()) {
            summation.add(iter.nextDouble());
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
    public OptionalDouble first() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final DoubleIterator iter = iteratorEx();

            return iter.hasNext() ? OptionalDouble.of(iter.nextDouble()) : OptionalDouble.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalDouble last() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final DoubleIterator iter = iteratorEx();

            if (!iter.hasNext()) {
                return OptionalDouble.empty();
            }

            double next = iter.nextDouble();

            while (iter.hasNext()) {
                next = iter.nextDouble();
            }

            return OptionalDouble.of(next);
        } finally {
            close();
        }
    }

    @SuppressWarnings("DuplicateThrows")
    @Override
    public OptionalDouble onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final DoubleIterator iter = iteratorEx();

            final OptionalDouble result = iter.hasNext() ? OptionalDouble.of(iter.nextDouble()) : OptionalDouble.empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new TooManyElementsException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.nextDouble()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalDouble findAny(final Throwables.DoublePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        return findFirst(predicate);
    }

    @Override
    public Optional<Map<Percentage, Double>> percentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final double[] a = sorted().toArray();

            if (a.length == 0) {
                return Optional.empty();
            }

            return Optional.of(N.percentilesOfSorted(a));
        } finally {
            close();
        }
    }

    @Override
    public Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> summaryStatisticsAndPercentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final double[] a = sorted().toArray();

            if (N.isEmpty(a)) {
                return Pair.of(new DoubleSummaryStatistics(), Optional.empty());
            } else {
                // For NaN-aware semantics consistent with DoubleSummaryStatistics.accept
                // (which uses Math.min/Math.max so any NaN propagates to both min and max),
                // detect NaN in the sorted array. With Double.compare ordering used by sort,
                // After sorting, NaN sorts to the end; guard both ends defensively.
                final double min;
                final double max;
                if (Double.isNaN(a[0]) || Double.isNaN(a[a.length - 1])) {
                    min = Double.NaN;
                    max = Double.NaN;
                } else {
                    min = a[0];
                    max = a[a.length - 1];
                }
                return Pair.of(new DoubleSummaryStatistics(a.length, min, max, sum(a)), Optional.of(N.percentilesOfSorted(a)));
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
            final DoubleIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextDouble());
            }

            return joiner.toString();
        } finally {
            close();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code joiner} is {@code null}
     */
    @Override
    public Joiner joinTo(final Joiner joiner) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(joiner, cs.joiner);

        try {
            @SuppressWarnings("resource")
            final DoubleIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextDouble());
            }
        } finally {
            close();
        }

        return joiner;
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjDoubleConsumer<? super R> accumulator) throws IllegalStateException {
        assertNotClosed();

        @SuppressWarnings("UnnecessaryLocalVariable")
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }

    @Override
    public DoubleIterator iterator() throws IllegalStateException {
        assertNotClosed();

        if (!isEmptyCloseHandlers(closeHandlers()) && logger.isWarnEnabled()) {
            logger.warn("Remember to close {} after iteration because it has close handlers", ClassUtil.getSimpleClassName(getClass()));
        }

        return iteratorEx();
    }
}

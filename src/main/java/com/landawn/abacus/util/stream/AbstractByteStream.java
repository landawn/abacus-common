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
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IndexedByte;
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
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteBiPredicate;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteTernaryOperator;
import com.landawn.abacus.util.function.ByteTriPredicate;
import com.landawn.abacus.util.function.ObjByteConsumer;
import com.landawn.abacus.util.function.ToByteFunction;

/**
 * Abstract base implementation of ByteStream providing common functionality for byte stream operations.
 * This class serves as the foundation for concrete byte stream implementations, offering intermediate
 * and terminal operations on sequences of primitive byte values.
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
 * ByteStream stream = new ArrayByteStream(new byte[] {1, 2, 3});
 * stream.filter(b -> b > 1).forEach(System.out::println);
 * }</pre>
 *
 */
abstract class AbstractByteStream extends ByteStream {

    /**
     * Constructs an AbstractByteStream with the specified sorting state and close handlers.
     *
     * @param sorted whether the stream elements are in sorted order
     * @param closeHandlers collection of handlers to execute when the stream is closed
     */
    AbstractByteStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);
    }

    @Override
    public ByteStream rateLimited(final RateLimiter rateLimiter) throws IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(rateLimiter, cs.rateLimiter);

        final ByteConsumer action = it -> rateLimiter.acquire();

        if (isParallel()) {
            //noinspection resource
            return sequential().onEach(action).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return onEach(action);
        }
    }

    @Override
    public ByteStream delay(final Duration delay) throws IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(delay, cs.delay);

        final long millis = delay.toMillis();

        final ByteConsumer action = new ByteConsumer() {
            private boolean isFirst = true;

            @Override
            public void accept(final byte it) {
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
    public ByteStream debounce(Duration duration) throws IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(duration, cs.duration);
        checkArgPositive(duration.toMillis(), cs.duration);

        if (isParallel()) {
            return psp(s -> s.debounce(duration));
        }

        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private final long durationMillis = duration.toMillis();
            private byte prev = 0; // the most recent element of the current burst, awaiting a quiet gap
            private boolean hasPrev = false;
            private long prevTime = 0;
            private byte next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                if (hasNext) {
                    return true;
                }

                while (iter.hasNext()) {
                    final byte val = iter.nextByte();
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
            public byte nextByte() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;
                return next;
            }
        }, isSorted());
    }

    @Override
    public ByteStream skipUntil(final BytePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return dropWhile(t -> !predicate.test(t));
    }

    @Override
    public ByteStream distinct() throws IllegalStateException {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        //noinspection resource
        return newStream(sequential().filter(set::add).iteratorEx(), isSorted());
    }

    @Override
    public ByteStream flatMapArray(final ByteFunction<byte[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMap(t -> ByteStream.of(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatmapToObj(final ByteFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatMapArrayToObj(final ByteFunction<T[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public ByteStream mapPartial(final ByteFunction<OptionalByte> mapper) {
        assertNotClosed();

        if (isParallel()) {
            //noinspection resource
            return mapToObj(mapper).psp(s -> s.filter(Fn.IS_PRESENT_BYTE).mapToByte(Fn.GET_AS_BYTE));
        } else {
            //noinspection resource
            return mapToObj(mapper).filter(Fn.IS_PRESENT_BYTE).mapToByte(Fn.GET_AS_BYTE);
        }
    }

    @Override
    public ByteStream rangeMap(final ByteBiPredicate sameRange, final ByteBinaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ByteIteratorEx() { //NOSONAR
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
    public <T> Stream<T> rangeMapToObj(final ByteBiPredicate sameRange, final ByteBiFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
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
    public Stream<ByteList> collapse(final ByteBiPredicate collapsible) throws IllegalStateException {
        assertNotClosed();

        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
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
    public ByteStream collapse(final ByteBiPredicate collapsible, final ByteBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private byte next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public byte nextByte() {
                byte merged = hasNext ? next : (next = iter.nextByte());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextByte()))) {
                        merged = mergeFunction.applyAsByte(merged, next);
                    } else {
                        break;
                    }
                }

                return merged;
            }
        }, false);
    }

    @Override
    public ByteStream collapse(final ByteTriPredicate collapsible, final ByteBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private byte next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public byte nextByte() {
                final byte first = hasNext ? next : (next = iter.nextByte());
                byte merged = first;

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.nextByte()))) {
                        merged = mergeFunction.applyAsByte(merged, next);
                    } else {
                        break;
                    }
                }

                return merged;
            }
        }, false);
    }

    @Override
    public ByteStream skip(final long n, final ByteConsumer action) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);
        checkArgNotNull(action, cs.action);

        if (n == 0) {
            return this;
        }

        final BytePredicate filter = isParallel() ? new BytePredicate() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(final byte value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new BytePredicate() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(final byte value) throws IllegalStateException {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public ByteStream filter(final BytePredicate predicate, final ByteConsumer onDrop) throws IllegalStateException {
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
    public ByteStream dropWhile(final BytePredicate predicate, final ByteConsumer onDrop) throws IllegalStateException {
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
    public ByteStream step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1) {
            return skip(0);
        }

        final long skip = step - 1;
        final ByteIteratorEx iter = iteratorEx();

        final ByteIterator byteIterator = new ByteIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                final byte next = iter.nextByte();
                iter.advance(skip);
                return next;
            }
        };

        return newStream(byteIterator, isSorted());
    }

    @Override
    public ByteStream scan(final ByteBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private byte accumulated = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                if (isFirst) {
                    isFirst = false;
                    return (accumulated = iter.nextByte());
                } else {
                    return (accumulated = accumulator.applyAsByte(accumulated, iter.nextByte()));
                }
            }
        }, false);
    }

    @Override
    public ByteStream scan(final byte init, final ByteBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private byte accumulated = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                return (accumulated = accumulator.applyAsByte(accumulated, iter.nextByte()));
            }
        }, false);
    }

    @Override
    public ByteStream scan(final byte init, final boolean initIncluded, final ByteBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (!initIncluded) {
            return scan(init, accumulator);
        }

        final ByteIteratorEx iter = iteratorEx();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private boolean isFirst = true;
            private byte accumulated = init;

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

                return (accumulated = accumulator.applyAsByte(accumulated, iter.nextByte()));
            }
        }, false);
    }

    @Override
    public ByteStream intersection(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(multiset::remove).iteratorEx(), isSorted());
    }

    @Override
    public ByteStream difference(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value)).iteratorEx(), isSorted());
    }

    @Override
    public ByteStream symmetricDifference(final Collection<? extends Byte> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value))
                .append(Stream.of(c).filter(multiset::remove).mapToByte(ToByteFunction.UNBOX))
                .iteratorEx(), false);
    }

    @Override
    public ByteStream reversed() throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private byte[] elements;
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
            public byte nextByte() {
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
            public byte[] toArray() {
                if (!initialized) {
                    init();
                }

                final byte[] a = new byte[cursor - fromIndex];

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = elements[cursor - i - 1];
                }

                cursor = fromIndex;

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<byte[], Integer, Integer> tuple = AbstractByteStream.this.arrayForIntermediateOp();

                    elements = tuple._1;
                    fromIndex = tuple._2;
                    toIndex = tuple._3;

                    cursor = toIndex;
                }
            }
        }, false);
    }

    @Override
    public ByteStream rotated(final int distance) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private byte[] elements;
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
            public byte nextByte() {
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
            public byte[] toArray() {
                if (!initialized) {
                    init();
                }

                final byte[] a = new byte[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = elements[((start + i) % len) + fromIndex];
                }

                cnt = len;

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<byte[], Integer, Integer> tuple = AbstractByteStream.this.arrayForIntermediateOp();

                    elements = tuple._1;
                    fromIndex = tuple._2;
                    toIndex = tuple._3;

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
    public ByteStream shuffled(final Random rnd) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(rnd, cs.rnd);

        return lazyLoad(a -> {
            N.shuffle(a, rnd);
            return a;
        }, false);
    }

    @Override
    public ByteStream sorted() throws IllegalStateException {
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
    public ByteStream reverseSorted() throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private boolean initialized = false;
            private byte[] sortedArray;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public byte nextByte() {
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
            public byte[] toArray() {
                if (!initialized) {
                    init();
                }

                final byte[] a = new byte[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = sortedArray[cursor - i - 1];
                }

                cursor = 0;

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    sortedArray = AbstractByteStream.this.toArrayForIntermediateOp();

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
     * Creates a lazily-loaded ByteStream by applying the given array transformation operation.
     * The stream materializes all elements into an array and applies {@code op} when the returned
     * stream is first consumed.
     *
     * @param op the transformation to apply to the collected element array
     * @param sorted whether the resulting stream should be marked as sorted
     * @return a new ByteStream backed by the transformed array
     */
    private ByteStream lazyLoad(final UnaryOperator<byte[]> op, final boolean sorted) {
        return ByteStream.defer(() -> newStream(op.apply(toArrayForIntermediateOp()), sorted)).onClose(this::close);
    }

    @Override
    public ByteStream cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIterator() { //NOSONAR
            private ByteIterator iter = null;
            private ByteList list = null;
            private byte[] a = null;
            private int len = 0;
            private int cursor = -1;
            private byte currentElement = 0;

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
            public byte nextByte() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (len > 0) {
                    if (cursor >= len) {
                        cursor = 0;
                    }

                    return a[cursor++];
                } else {
                    currentElement = iter.nextByte();
                    list.add(currentElement);

                    return currentElement;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractByteStream.this.iteratorEx();
                    list = new ByteList();
                }
            }
        }, false);
    }

    @Override
    public ByteStream cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new ByteIterator() { //NOSONAR
            private ByteIterator iter = null;
            private ByteList list = null;
            private byte[] a = null;
            private int len = 0;
            private int cursor = -1;
            private byte currentElement = 0;
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
                    // otherwise rounds >= 3 would have hasNext() return true while nextByte()
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
            public byte nextByte() {
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
                    currentElement = iter.nextByte();
                    list.add(currentElement);

                    return currentElement;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractByteStream.this.iteratorEx();
                    list = new ByteList();
                }
            }
        }, rounds <= 1 && isSorted());
    }

    @Override
    public Stream<IndexedByte> indexed() throws IllegalStateException {
        assertNotClosed();

        final MutableLong idx = MutableLong.of(0);

        //noinspection resource
        return newStream(sequential().mapToObj(t -> IndexedByte.of(t, idx.getAndIncrement())).iteratorEx(), true, INDEXED_BYTE_COMPARATOR);
    }

    @Override
    public Stream<Byte> boxed() throws IllegalStateException {
        assertNotClosed();

        return newStream(iteratorEx(), isSorted(), isSorted() ? BYTE_COMPARATOR : null);
    }

    @Override
    public final ByteStream prepend(final byte... a) throws IllegalStateException {
        assertNotClosed();

        return prepend(ByteStream.of(a));
    }

    @Override
    public ByteStream prepend(final ByteStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return ByteStream.concat(stream, this).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return ByteStream.concat(stream, this);
        }
    }

    @Override
    public ByteStream prepend(final OptionalByte op) throws IllegalStateException {
        assertNotClosed();

        // return prepend(op.stream());
        return op.isEmpty() ? this : prepend(op.orElseThrow());
    }

    @Override
    public final ByteStream append(final byte... a) throws IllegalStateException {
        assertNotClosed();

        return append(ByteStream.of(a));
    }

    @Override
    public ByteStream append(final ByteStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return ByteStream.concat(this, stream).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return ByteStream.concat(this, stream);
        }
    }

    @Override
    public ByteStream append(final OptionalByte op) { //NOSONAR
        assertNotClosed();

        // return append(op.stream());
        return op.isEmpty() ? this : append(op.orElseThrow());
    }

    @Override
    public final ByteStream appendIfEmpty(final byte... a) throws IllegalStateException {
        assertNotClosed();

        return appendIfEmpty(() -> ByteStream.of(a));
    }

    @Override
    public ByteStream mergeWith(final ByteStream b, final ByteBiFunction<MergeResult> nextSelector) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return ByteStream.merge(this, b, nextSelector).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return ByteStream.merge(this, b, nextSelector);
        }
    }

    @Override
    public ByteStream zipWith(final ByteStream b, final ByteBinaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return ByteStream.zip(this, b, zipFunction);
    }

    @Override
    public ByteStream zipWith(final ByteStream b, final ByteStream c, final ByteTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return ByteStream.zip(this, b, c, zipFunction);
    }

    @Override
    public ByteStream zipWith(final ByteStream b, final byte valueForNoneA, final byte valueForNoneB, final ByteBinaryOperator zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        return ByteStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public ByteStream zipWith(final ByteStream b, final ByteStream c, final byte valueForNoneA, final byte valueForNoneB, final byte valueForNoneC,
            final ByteTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return ByteStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.ByteFunction<? extends K, E> keyMapper,
            final Throwables.ByteFunction<? extends V, E2> valueMapper) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.ByteFunction<? extends K, E> keyMapper,
            final Throwables.ByteFunction<? extends V, E2> valueMapper, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Fn.throwingMerger(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.ByteFunction<? extends K, E> keyMapper,
            final Throwables.ByteFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    @Override
    public <K, D, E extends Exception> Map<K, D> groupTo(final Throwables.ByteFunction<? extends K, E> keyMapper,
            final Collector<? super Byte, ?, D> downstream) throws IllegalStateException, E {
        assertNotClosed();

        return groupTo(keyMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <E extends Exception> void forEachIndexed(final Throwables.IntByteConsumer<E> action) throws IllegalStateException, E {
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
    public OptionalByte first() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final ByteIterator iter = iteratorEx();

            return iter.hasNext() ? OptionalByte.of(iter.nextByte()) : OptionalByte.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalByte last() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final ByteIterator iter = iteratorEx();

            if (!iter.hasNext()) {
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

    @SuppressWarnings("DuplicateThrows")
    @Override
    public OptionalByte onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final ByteIterator iter = iteratorEx();

            final OptionalByte result = iter.hasNext() ? OptionalByte.of(iter.nextByte()) : OptionalByte.empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new TooManyElementsException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.nextByte()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalByte findAny(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        return findFirst(predicate);
    }

    @Override
    public Optional<Map<Percentage, Byte>> percentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final byte[] a = sorted().toArray();

            if (a.length == 0) {
                return Optional.empty();
            }

            return Optional.of(N.percentilesOfSorted(a));
        } finally {
            close();
        }
    }

    @Override
    public Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> summaryStatisticsAndPercentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final byte[] a = sorted().toArray();

            if (N.isEmpty(a)) {
                return Pair.of(new ByteSummaryStatistics(), Optional.empty());
            } else {
                // Compute sum as a long locally; StreamBase.sum(byte[]) wraps the result in
                // toIntExact and throws ArithmeticException on overflow, but ByteSummaryStatistics's
                // 4th argument is a long and the analogous summaryStatistics() path accumulates
                // into a long without overflow. Diverging behavior here would cause one API to
                // throw on the same data the other handles cleanly.
                long s = 0;
                for (byte v : a) {
                    s += v;
                }
                return Pair.of(new ByteSummaryStatistics(a.length, a[0], a[a.length - 1], s), Optional.of(N.percentilesOfSorted(a)));
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
            final ByteIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextByte());
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
            final ByteIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextByte());
            }
        } finally {
            close();
        }

        return joiner;
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjByteConsumer<? super R> accumulator) throws IllegalStateException {
        assertNotClosed();

        @SuppressWarnings("UnnecessaryLocalVariable")
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }

    @Override
    public ByteIterator iterator() throws IllegalStateException {
        assertNotClosed();

        if (!isEmptyCloseHandlers(closeHandlers()) && logger.isWarnEnabled()) {
            logger.warn("Remember to close {} after iteration because it has close handlers", ClassUtil.getSimpleClassName(getClass()));
        }

        return iteratorEx();
    }
}

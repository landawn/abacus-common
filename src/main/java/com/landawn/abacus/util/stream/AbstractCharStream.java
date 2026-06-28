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
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IndexedChar;
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
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharTernaryOperator;
import com.landawn.abacus.util.function.CharTriPredicate;
import com.landawn.abacus.util.function.ObjCharConsumer;
import com.landawn.abacus.util.function.ToCharFunction;

/**
 * Abstract base implementation of CharStream providing common functionality for char stream operations.
 * This class serves as the foundation for concrete char stream implementations, offering intermediate
 * and terminal operations on sequences of primitive char values.
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
 * CharStream stream = new ArrayCharStream(new char[] {'a', 'b', 'c'});
 * stream.filter(c -> c > 'a').forEach(System.out::println);
 * }</pre>
 *
 */
abstract class AbstractCharStream extends CharStream {

    /**
     * Constructs an AbstractCharStream with the specified sorting state and close handlers.
     *
     * @param sorted whether the stream elements are in sorted order
     * @param closeHandlers collection of handlers to execute when the stream is closed
     */
    AbstractCharStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);
    }

    @Override
    public CharStream rateLimited(final RateLimiter rateLimiter) throws IllegalArgumentException {
        checkArgNotNull(rateLimiter, cs.rateLimiter);

        final CharConsumer action = it -> rateLimiter.acquire();

        if (isParallel()) {
            //noinspection resource
            return sequential().onEach(action).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return onEach(action);
        }
    }

    @Override
    public CharStream delay(final Duration delay) throws IllegalArgumentException {
        checkArgNotNull(delay, cs.delay);

        final long millis = delay.toMillis();

        final CharConsumer action = new CharConsumer() {
            private boolean isFirst = true;

            @Override
            public void accept(final char it) {
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
    public CharStream debounce(Duration duration) {
        checkArgNotNull(duration, cs.duration);
        checkArgPositive(duration.toMillis(), cs.duration);

        if (isParallel()) {
            return psp(s -> s.debounce(duration));
        }

        final CharIteratorEx iter = iteratorEx();

        return newStream(new CharIteratorEx() {
            private final long durationMillis = duration.toMillis();
            private char prev = 0; // the most recent element of the current burst, awaiting a quiet gap
            private boolean hasPrev = false;
            private long prevTime = 0;
            private char next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                if (hasNext) {
                    return true;
                }

                while (iter.hasNext()) {
                    final char val = iter.nextChar();
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
            public char nextChar() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;
                return next;
            }
        }, isSorted());
    }

    @Override
    public CharStream skipUntil(final CharPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return dropWhile(t -> !predicate.test(t));
    }

    @Override
    public CharStream distinct() throws IllegalStateException {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        //noinspection resource
        return newStream(sequential().filter(set::add).iteratorEx(), isSorted());
    }

    @Override
    public CharStream flatMapArray(final CharFunction<char[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMap(t -> CharStream.of(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatmapToObj(final CharFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public <T> Stream<T> flatMapArrayToObj(final CharFunction<T[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToObj(t -> Stream.of(mapper.apply(t)));
    }

    @Override
    public CharStream mapPartial(final CharFunction<OptionalChar> mapper) {
        if (isParallel()) {
            //noinspection resource
            return mapToObj(mapper).psp(s -> s.filter(Fn.IS_PRESENT_CHAR).mapToChar(Fn.GET_AS_CHAR));
        } else {
            //noinspection resource
            return mapToObj(mapper).filter(Fn.IS_PRESENT_CHAR).mapToChar(Fn.GET_AS_CHAR);
        }
    }

    @Override
    public CharStream rangeMap(final CharBiPredicate sameRange, final CharBinaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        final CharIteratorEx iter = iteratorEx();

        return newStream(new CharIteratorEx() { //NOSONAR
            private char left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public char nextChar() {
                left = hasNext ? next : iter.nextChar();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextChar();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.applyAsChar(left, right);
            }
        }, false);
    }

    @Override
    public <T> Stream<T> rangeMapToObj(final CharBiPredicate sameRange, final CharBiFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        final CharIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private char left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                left = hasNext ? next : iter.nextChar();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextChar();

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
    public Stream<CharList> collapse(final CharBiPredicate collapsible) throws IllegalStateException {
        assertNotClosed();

        final CharIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private char next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public CharList next() {
                final CharList result = new CharList(9);
                result.add(hasNext ? next : (next = iter.nextChar()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextChar()))) {
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
    public CharStream collapse(final CharBiPredicate collapsible, final CharBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final CharIteratorEx iter = iteratorEx();

        return newStream(new CharIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private char next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public char nextChar() {
                char merged = hasNext ? next : (next = iter.nextChar());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextChar()))) {
                        merged = mergeFunction.applyAsChar(merged, next);
                    } else {
                        break;
                    }
                }

                return merged;
            }
        }, false);
    }

    @Override
    public CharStream collapse(final CharTriPredicate collapsible, final CharBinaryOperator mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final CharIteratorEx iter = iteratorEx();

        return newStream(new CharIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private char next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public char nextChar() {
                final char first = hasNext ? next : (next = iter.nextChar());
                char merged = first;

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.nextChar()))) {
                        merged = mergeFunction.applyAsChar(merged, next);
                    } else {
                        break;
                    }
                }

                return merged;
            }
        }, false);
    }

    @Override
    public CharStream skip(final long n, final CharConsumer action) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);
        checkArgNotNull(action, cs.action);

        if (n == 0) {
            return this;
        }

        final CharPredicate filter = isParallel() ? new CharPredicate() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(final char value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new CharPredicate() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(final char value) throws IllegalStateException {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public CharStream filter(final CharPredicate predicate, final CharConsumer onDrop) throws IllegalStateException {
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
    public CharStream dropWhile(final CharPredicate predicate, final CharConsumer onDrop) throws IllegalStateException {
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
    public CharStream step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1) {
            return skip(0);
        }

        final long skip = step - 1;
        final CharIteratorEx iter = iteratorEx();

        final CharIterator charIterator = new CharIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public char nextChar() {
                final char next = iter.nextChar();
                iter.advance(skip);
                return next;
            }
        };

        return newStream(charIterator, isSorted());
    }

    @Override
    public CharStream scan(final CharBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final CharIteratorEx iter = iteratorEx();

        return newStream(new CharIteratorEx() { //NOSONAR
            private char accumulated = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public char nextChar() {
                if (isFirst) {
                    isFirst = false;
                    return (accumulated = iter.nextChar());
                } else {
                    return (accumulated = accumulator.applyAsChar(accumulated, iter.nextChar()));
                }
            }
        }, false);
    }

    @Override
    public CharStream scan(final char init, final CharBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        final CharIteratorEx iter = iteratorEx();

        return newStream(new CharIteratorEx() { //NOSONAR
            private char accumulated = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public char nextChar() {
                return (accumulated = accumulator.applyAsChar(accumulated, iter.nextChar()));
            }
        }, false);
    }

    @Override
    public CharStream scan(final char init, final boolean initIncluded, final CharBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (!initIncluded) {
            return scan(init, accumulator);
        }

        final CharIteratorEx iter = iteratorEx();

        return newStream(new CharIteratorEx() { //NOSONAR
            private boolean isFirst = true;
            private char accumulated = init;

            @Override
            public boolean hasNext() {
                return isFirst || iter.hasNext();
            }

            @Override
            public char nextChar() {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (accumulated = accumulator.applyAsChar(accumulated, iter.nextChar()));
            }
        }, false);
    }

    @Override
    public CharStream intersection(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(multiset::remove).iteratorEx(), isSorted());
    }

    @Override
    public CharStream difference(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value)).iteratorEx(), isSorted());
    }

    @Override
    public CharStream symmetricDifference(final Collection<? extends Character> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value))
                .append(Stream.of(c).filter(multiset::remove).mapToChar(ToCharFunction.UNBOX))
                .iteratorEx(), false);
    }

    @Override
    public CharStream reversed() throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private char[] elements;
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
            public char nextChar() {
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
            public char[] toArray() {
                if (!initialized) {
                    init();
                }

                final char[] a = new char[cursor - fromIndex];

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = elements[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<char[], Integer, Integer> tp = AbstractCharStream.this.arrayForIntermediateOp();

                    elements = tp._1;
                    fromIndex = tp._2;
                    toIndex = tp._3;

                    cursor = toIndex;
                }
            }
        }, false);
    }

    @Override
    public CharStream rotated(final int distance) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            private boolean initialized = false;

            private char[] elements;
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
            public char nextChar() {
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
            public char[] toArray() {
                if (!initialized) {
                    init();
                }

                final char[] a = new char[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = elements[((start + i) % len) + fromIndex];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<char[], Integer, Integer> tp = AbstractCharStream.this.arrayForIntermediateOp();

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
    public CharStream shuffled(final Random rnd) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(rnd, cs.rnd);

        return lazyLoad(a -> {
            N.shuffle(a, rnd);
            return a;
        }, false);
    }

    @Override
    public CharStream sorted() throws IllegalStateException {
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
    public CharStream reverseSorted() throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            private boolean initialized = false;
            private char[] sortedArray;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public char nextChar() {
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
            public char[] toArray() {
                if (!initialized) {
                    init();
                }

                final char[] a = new char[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = sortedArray[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    sortedArray = AbstractCharStream.this.toArrayForIntermediateOp();

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
     * Creates a lazily-loaded CharStream by applying the given array transformation operation.
     * The stream materializes all elements into an array and applies {@code op} when the returned
     * stream is first consumed.
     *
     * @param op the transformation to apply to the collected element array
     * @param sorted whether the resulting stream should be marked as sorted
     * @return a new CharStream backed by the transformed array
     */
    private CharStream lazyLoad(final UnaryOperator<char[]> op, final boolean sorted) {
        return CharStream.defer(() -> newStream(op.apply(toArrayForIntermediateOp()), sorted)).onClose(this::close);
    }

    @Override
    public CharStream cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIterator() { //NOSONAR
            private CharIterator iter = null;
            private CharList list = null;
            private char[] a = null;
            private int len = 0;
            private int cursor = -1;
            private char e = 0;

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
            public char nextChar() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (len > 0) {
                    if (cursor >= len) {
                        cursor = 0;
                    }

                    return a[cursor++];
                } else {
                    e = iter.nextChar();
                    list.add(e);

                    return e;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractCharStream.this.iteratorEx();
                    list = new CharList();
                }
            }
        }, false);
    }

    @Override
    public CharStream cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new CharIterator() { //NOSONAR
            private CharIterator iter = null;
            private CharList list = null;
            private char[] a = null;
            private int len = 0;
            private int cursor = -1;
            private char e = 0;
            private long m = 0;

            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                if (m >= rounds) {
                    return false;
                }

                if (a != null) {
                    // len == 0 means the source produced zero elements; cycling an empty
                    // stream any number of times yields nothing — must short-circuit here,
                    // otherwise rounds >= 3 would have hasNext() return true while nextChar()
                    // throws NoSuchElementException on the exhausted iter.
                    return len > 0 && (cursor < len || rounds - m > 1);
                } else if (iter.hasNext()) {
                    return true;
                } else {
                    a = list.toArray();
                    len = a.length;
                    cursor = 0;
                    m++;

                    return m < rounds && len > 0;
                }
            }

            @Override
            public char nextChar() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (len > 0) {
                    if (cursor >= len) {
                        cursor = 0;
                        m++;
                    }

                    return a[cursor++];
                } else {
                    e = iter.nextChar();
                    list.add(e);

                    return e;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractCharStream.this.iteratorEx();
                    list = new CharList();
                }
            }
        }, rounds <= 1 && isSorted());
    }

    @Override
    public Stream<IndexedChar> indexed() throws IllegalStateException {
        assertNotClosed();

        final MutableLong idx = MutableLong.of(0);

        //noinspection resource
        return newStream(sequential().mapToObj(t -> IndexedChar.of(t, idx.getAndIncrement())).iteratorEx(), true, INDEXED_CHAR_COMPARATOR);
    }

    @Override
    public Stream<Character> boxed() throws IllegalStateException {
        assertNotClosed();

        return newStream(iteratorEx(), isSorted(), isSorted() ? CHAR_COMPARATOR : null);
    }

    @Override
    public final CharStream prepend(final char... a) throws IllegalStateException {
        assertNotClosed();

        return prepend(CharStream.of(a));
    }

    @Override
    public CharStream prepend(final CharStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return CharStream.concat(stream, this).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return CharStream.concat(stream, this);
        }
    }

    @Override
    public CharStream prepend(final OptionalChar op) throws IllegalStateException {
        assertNotClosed();

        // return prepend(op.stream());
        return op.isEmpty() ? this : prepend(op.orElseThrow());
    }

    @Override
    public final CharStream append(final char... a) throws IllegalStateException {
        assertNotClosed();

        return append(CharStream.of(a));
    }

    @Override
    public CharStream append(final CharStream stream) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return CharStream.concat(this, stream).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return CharStream.concat(this, stream);
        }
    }

    @Override
    public CharStream append(final OptionalChar op) { //NOSONAR
        assertNotClosed();

        // return append(op.stream());
        return op.isEmpty() ? this : append(op.orElseThrow());
    }

    @Override
    public final CharStream appendIfEmpty(final char... a) throws IllegalStateException {
        assertNotClosed();

        return appendIfEmpty(() -> CharStream.of(a));
    }

    @Override
    public CharStream mergeWith(final CharStream b, final CharBiFunction<MergeResult> nextSelector) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return CharStream.merge(this, b, nextSelector).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return CharStream.merge(this, b, nextSelector);
        }
    }

    @Override
    public CharStream zipWith(final CharStream b, final CharBinaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return CharStream.zip(this, b, zipFunction);
    }

    @Override
    public CharStream zipWith(final CharStream b, final CharStream c, final CharTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return CharStream.zip(this, b, c, zipFunction);
    }

    @Override
    public CharStream zipWith(final CharStream b, final char valueForNoneA, final char valueForNoneB, final CharBinaryOperator zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        return CharStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public CharStream zipWith(final CharStream b, final CharStream c, final char valueForNoneA, final char valueForNoneB, final char valueForNoneC,
            final CharTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        return CharStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.CharFunction<? extends K, E> keyMapper,
            final Throwables.CharFunction<? extends V, E2> valueMapper) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.CharFunction<? extends K, E> keyMapper,
            final Throwables.CharFunction<? extends V, E2> valueMapper, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Fn.throwingMerger(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.CharFunction<? extends K, E> keyMapper,
            final Throwables.CharFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    @Override
    public <K, D, E extends Exception> Map<K, D> groupTo(final Throwables.CharFunction<? extends K, E> keyMapper,
            final Collector<? super Character, ?, D> downstream) throws E {
        assertNotClosed();

        return groupTo(keyMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <E extends Exception> void forEachIndexed(final Throwables.IntCharConsumer<E> action) throws IllegalStateException, E {
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
    public OptionalChar first() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final CharIterator iter = iteratorEx();

            return iter.hasNext() ? OptionalChar.of(iter.nextChar()) : OptionalChar.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalChar last() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final CharIterator iter = iteratorEx();

            if (!iter.hasNext()) {
                return OptionalChar.empty();
            }

            char next = iter.nextChar();

            while (iter.hasNext()) {
                next = iter.nextChar();
            }

            return OptionalChar.of(next);
        } finally {
            close();
        }
    }

    @SuppressWarnings("DuplicateThrows")
    @Override
    public OptionalChar onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final CharIterator iter = iteratorEx();

            final OptionalChar result = iter.hasNext() ? OptionalChar.of(iter.nextChar()) : OptionalChar.empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new TooManyElementsException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.nextChar()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalChar findAny(final Throwables.CharPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        return findFirst(predicate);
    }

    @Override
    public Optional<Map<Percentage, Character>> percentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final char[] a = sorted().toArray();

            if (a.length == 0) {
                return Optional.empty();
            }

            return Optional.of(N.percentilesOfSorted(a));
        } finally {
            close();
        }
    }

    @Override
    public Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> summaryStatisticsAndPercentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final char[] a = sorted().toArray();

            if (N.isEmpty(a)) {
                return Pair.of(new CharSummaryStatistics(), Optional.empty());
            } else {
                // Compute sum as a long locally; StreamBase.sum(char[]) wraps the result in
                // toIntExact and throws ArithmeticException on overflow, but CharSummaryStatistics's
                // 4th argument is a long and the analogous summaryStatistics() path accumulates
                // into a long without overflow. Diverging behavior here would cause one API to
                // throw on the same data the other handles cleanly.
                long s = 0;
                for (char v : a) {
                    s += v;
                }
                return Pair.of(new CharSummaryStatistics(a.length, a[0], a[a.length - 1], s), Optional.of(N.percentilesOfSorted(a)));
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
            final CharIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextChar());
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
            final CharIteratorEx iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextChar());
            }
        } finally {
            close();
        }

        return joiner;
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjCharConsumer<? super R> accumulator) throws IllegalStateException {
        assertNotClosed();

        @SuppressWarnings("UnnecessaryLocalVariable")
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }

    @Override
    public CharIterator iterator() throws IllegalStateException {
        assertNotClosed();

        if (!isEmptyCloseHandlers(closeHandlers()) && logger.isWarnEnabled()) {
            logger.warn("Remember to close {} after iteration because it has close handlers", ClassUtil.getSimpleClassName(getClass()));
        }

        return iteratorEx();
    }
}

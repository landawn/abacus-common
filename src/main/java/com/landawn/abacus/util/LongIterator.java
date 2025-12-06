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

package com.landawn.abacus.util;

import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.LongStream;

/**
 * A specialized iterator for primitive long values. This class provides an efficient way to iterate over
 * long values without the overhead of boxing/unboxing that comes with using Iterator&lt;Long&gt;.
 * 
 * <p>This abstract class extends ImmutableIterator to ensure that the remove() operation is not supported,
 * making all LongIterator instances immutable in terms of structural modification.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L);
 * while (iter.hasNext()) {
 *     System.out.println(iter.nextLong());
 * }
 * }</pre>
 *
 * @see ObjIterator
 * @see BiIterator
 * @see TriIterator
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 */
@SuppressWarnings({ "java:S6548" })
public abstract class LongIterator extends ImmutableIterator<Long> {

    /**
     * Protected constructor for subclasses.
     */
    protected LongIterator() {
    }

    /**
     * An empty LongIterator that always returns {@code false} for hasNext() and throws
     * NoSuchElementException for nextLong(). This constant is useful for representing
     * an iterator over an empty collection of longs.
     */
    public static final LongIterator EMPTY = new LongIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public long nextLong() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     * Returns an empty {@code LongIterator} with no elements.
     *
     * <p>The returned iterator's {@code hasNext()} will always return {@code false},
     * and calling {@code nextLong()} will always throw a {@code NoSuchElementException}.</p>
     *
     * <p>This method always returns the same singleton instance for efficiency.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator iter = LongIterator.empty();
     * System.out.println(iter.hasNext());   // false
     * }</pre>
     *
     * @return an empty {@code LongIterator}
     */
    @SuppressWarnings("SameReturnValue")
    public static LongIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     * Creates a {@code LongIterator} from the specified long array.
     *
     * <p>If the array is {@code null} or empty, returns an empty iterator.
     * The iterator will iterate over all elements in the array from start to end.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator iter = LongIterator.of(10L, 20L, 30L);
     * // Will iterate over: 10, 20, 30
     * }</pre>
     *
     * @param a the long array (may be {@code null})
     * @return a new {@code LongIterator} over the array elements, or an empty iterator if the array is {@code null} or empty
     */
    public static LongIterator of(final long... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a {@code LongIterator} from a subsequence of the specified long array.
     *
     * <p>The iterator will iterate over elements from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive). If {@code fromIndex} equals {@code toIndex}, an empty
     * iterator is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] array = {1L, 2L, 3L, 4L, 5L};
     * LongIterator iter = LongIterator.of(array, 1, 4);
     * // Will iterate over: 2, 3, 4
     * }</pre>
     *
     * @param a the long array (may be {@code null})
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a new {@code LongIterator} over the specified range, or an empty iterator if the array is {@code null} or fromIndex equals toIndex
     * @throws IndexOutOfBoundsException if fromIndex is negative, toIndex is greater than the array length,
     *         or fromIndex is greater than toIndex
     */
    public static LongIterator of(final long[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        }

        return new LongIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public long nextLong() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }

            @Override
            public long[] toArray() {
                final long[] ret = N.copyOfRange(a, cursor, toIndex);
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

            @Override
            public LongList toList() {
                final LongList ret = LongList.of(N.copyOfRange(a, cursor, toIndex));
                cursor = toIndex; // Mark as exhausted
                return ret;
            }
        };
    }

    /**
     * Creates a LongIterator that is initialized lazily using the provided Supplier.
     * The actual iterator is not created until the first method call (hasNext() or nextLong()).
     * This is useful for deferring expensive iterator creation until it's actually needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator iter = LongIterator.defer(() -> LongIterator.of(expensiveMethod()));
     * // expensiveMethod() is not called until iter.hasNext() or iter.nextLong() is invoked
     * }</pre>
     *
     * @param iteratorSupplier a Supplier that provides the LongIterator when needed
     * @return a LongIterator that delegates to the iterator provided by the supplier
     * @throws IllegalArgumentException if iteratorSupplier is null
     */
    public static LongIterator defer(final Supplier<? extends LongIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new LongIterator() {
            private LongIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextLong();
            }

            private void init() {
                if (!isInitialized) {
                    isInitialized = true;
                    iter = iteratorSupplier.get();
                }
            }
        };
    }

    /**
     * Creates an infinite LongIterator that generates values using the provided LongSupplier.
     * The iterator will always return {@code true} for hasNext() and will generate values on demand.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator randomLongs = LongIterator.generate(() -> ThreadLocalRandom.current().nextLong());
     * // Generates random long values infinitely
     * }</pre>
     *
     * @param supplier the LongSupplier used to generate values
     * @return an infinite LongIterator that generates values using the supplier
     * @throws IllegalArgumentException if supplier is null
     */
    public static LongIterator generate(final LongSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier);

        return new LongIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public long nextLong() {
                return supplier.getAsLong();
            }
        };
    }

    /**
     * Creates a LongIterator that generates values using the provided LongSupplier while the
     * BooleanSupplier returns {@code true}. This allows for creating finite iterators with dynamic termination conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int count = 0;
     * LongIterator iter = LongIterator.generate(
     *     () -> count < 5,
     *     () -> count++
     * );
     * // Will generate: 0, 1, 2, 3, 4
     * }</pre>
     *
     * @param hasNext a BooleanSupplier that determines if more elements are available
     * @param supplier the LongSupplier used to generate values
     * @return a LongIterator that generates values while hasNext returns true
     * @throws IllegalArgumentException if hasNext or supplier is null
     */
    public static LongIterator generate(final BooleanSupplier hasNext, final LongSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new LongIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return supplier.getAsLong();
            }
        };
    }

    /**
     * Returns the next element as a boxed Long. This method is provided for compatibility
     * with the Iterator interface but should be avoided in favor of nextLong() for better performance.
     *
     * @return the next long value as a boxed Long
     * @throws NoSuchElementException if the iteration has no more elements
     * @deprecated use {@code nextLong()} instead to avoid boxing overhead
     */
    @Deprecated
    @Override
    public Long next() {
        return nextLong();
    }

    /**
     * Returns the next long value in the iteration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator iter = LongIterator.of(1L, 2L, 3L);
     * long first = iter.nextLong();    // 1
     * long second = iter.nextLong();   // 2
     * }</pre>
     *
     * @return the next long value
     * @throws NoSuchElementException if the iteration has no more elements
     */
    public abstract long nextLong();

    /**
     * Returns a new LongIterator that skips the first n elements. If n is greater than
     * the number of remaining elements, all elements are skipped.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L).skip(2);
     * // Will iterate over: 3, 4, 5
     * }</pre>
     *
     * @param n the number of elements to skip
     * @return a new LongIterator that skips the first n elements
     * @throws IllegalArgumentException if n is negative
     */
    public LongIterator skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final LongIterator iter = this;

        return new LongIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextLong();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextLong();
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns a new LongIterator that will iterate over at most count elements.
     * If count is 0, an empty iterator is returned. If count is greater than the number
     * of remaining elements, all remaining elements are included.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L).limit(3);
     * // Will iterate over: 1, 2, 3
     * }</pre>
     *
     * @param count the maximum number of elements to iterate over
     * @return a new LongIterator limited to count elements
     * @throws IllegalArgumentException if count is negative
     */
    public LongIterator limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return LongIterator.EMPTY;
        }

        final LongIterator iter = this;

        return new LongIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.nextLong();
            }
        };
    }

    /**
     * Returns a new LongIterator that only includes elements matching the given predicate.
     * Elements that don't match the predicate are skipped.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator iter = LongIterator.of(1L, 2L, 3L, 4L, 5L)
     *     .filter(x -> x % 2 == 0);
     * // Will iterate over: 2, 4
     * }</pre>
     *
     * @param predicate the predicate used to test elements
     * @return a new LongIterator containing only elements that match the predicate
     * @throws IllegalArgumentException if predicate is null
     */
    public LongIterator filter(final LongPredicate predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate);

        final LongIterator iter = this;

        return new LongIterator() {
            private boolean hasNext = false;
            private long next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        next = iter.nextLong();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public long nextLong() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        };
    }

    /**
     * Returns an OptionalLong containing the first element, or an empty OptionalLong if
     * this iterator is empty. This method consumes the first element if present.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalLong first = LongIterator.of(1L, 2L, 3L).first();
     * // first.get() returns 1L
     * }</pre>
     *
     * @return an OptionalLong containing the first element, or empty if no elements exist
     */
    public OptionalLong first() {
        if (hasNext()) {
            return OptionalLong.of(nextLong());
        } else {
            return OptionalLong.empty();
        }
    }

    /**
     * Returns an OptionalLong containing the last element, or an empty OptionalLong if
     * this iterator is empty. This method consumes all elements in the iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalLong last = LongIterator.of(1L, 2L, 3L).last();
     * // last.get() returns 3L
     * }</pre>
     *
     * @return an OptionalLong containing the last element, or empty if no elements exist
     */
    public OptionalLong last() {
        if (hasNext()) {
            long next = nextLong();

            while (hasNext()) {
                next = nextLong();
            }

            return OptionalLong.of(next);
        } else {
            return OptionalLong.empty();
        }
    }

    /**
     * Converts the remaining elements to a long array.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] array = LongIterator.of(1L, 2L, 3L, 4L, 5L).toArray();
     * // array = [1, 2, 3, 4, 5]
     *
     * // Empty iterator returns empty array
     * long[] empty = LongIterator.empty().toArray();   // empty.length == 0
     * }</pre>
     *
     * @return a long array containing all remaining elements
     */
    @SuppressWarnings("deprecation")
    public long[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     * Converts the remaining elements to a LongList.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty LongList.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongList list = LongIterator.of(1L, 2L, 3L, 4L, 5L).toList();
     * // list contains [1, 2, 3, 4, 5]
     *
     * // Empty iterator returns empty list
     * LongList empty = LongIterator.empty().toList();   // empty.size() == 0
     * }</pre>
     *
     * @return a LongList containing all remaining elements
     */
    public LongList toList() {
        final LongList list = new LongList();

        while (hasNext()) {
            list.add(nextLong());
        }

        return list;
    }

    /**
     * Converts this iterator to a LongStream. The stream will be sequential and
     * will consume elements from this iterator as needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long sum = LongIterator.of(1L, 2L, 3L).stream().sum();
     * // sum is 6
     * }</pre>
     *
     * @return a LongStream backed by this iterator
     */
    public LongStream stream() {
        return LongStream.of(this);
    }

    /**
     * Returns an iterator of IndexedLong objects, where each element is paired with its index
     * starting from 0. This is useful when you need both the value and its position.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<IndexedLong> iter = LongIterator.of(10L, 20L, 30L).indexed();
     * // Will produce: IndexedLong(10, 0), IndexedLong(20, 1), IndexedLong(30, 2)
     * }</pre>
     *
     * @return an ObjIterator of IndexedLong objects
     */
    @Beta
    public ObjIterator<IndexedLong> indexed() {
        return indexed(0);
    }

    /**
     * Returns an iterator of IndexedLong objects, where each element is paired with its index
     * starting from the specified startIndex. This is useful when you need both the value and 
     * its position with a custom starting index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIterator<IndexedLong> iter = LongIterator.of(10L, 20L, 30L).indexed(5);
     * // Will produce: IndexedLong(10, 5), IndexedLong(20, 6), IndexedLong(30, 7)
     * }</pre>
     *
     * @param startIndex the starting index value
     * @return an ObjIterator of IndexedLong objects with indices starting from startIndex
     * @throws IllegalArgumentException if startIndex is negative
     */
    @Beta
    public ObjIterator<IndexedLong> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final LongIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedLong next() {
                return IndexedLong.of(iter.nextLong(), idx++);
            }
        };
    }

    /**
     * Performs the given action for each remaining element. This method is provided for
     * compatibility with the Iterator interface but should be avoided in favor of
     * foreachRemaining(LongConsumer) for better performance.
     *
     * <p>This method consumes all remaining elements in the iterator. After calling this method,
     * the iterator will be empty (hasNext() returns false).</p>
     *
     * @param action the action to be performed for each element
     * @deprecated use {@link #foreachRemaining(Throwables.LongConsumer)} instead to avoid boxing
     */
    @Deprecated
    @Override
    public void forEachRemaining(final java.util.function.Consumer<? super Long> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining element. The action is performed in the order
     * of iteration, if that order is specified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator.of(1L, 2L, 3L).foreachRemaining(System.out::println);
     * // Prints: 1, 2, 3
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each element
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachRemaining(final Throwables.LongConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextLong());
        }
    }

    /**
     * Performs the given action for each remaining element, providing both the element's index
     * and value. The index starts from 0 and increments for each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator.of(10L, 20L, 30L).foreachIndexed((index, value) -> 
     *     System.out.println("Index: " + index + ", Value: " + value)
     * );
     * // Prints:
     * // Index: 0, Value: 10
     * // Index: 1, Value: 20
     * // Index: 2, Value: 30
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each element, accepting index and value
     * @throws IllegalArgumentException if action is null
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntLongConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            if (idx < 0) {
                throw new IllegalStateException("Index overflow: iterator has more than Integer.MAX_VALUE elements");
            }
            action.accept(idx++, nextLong());
        }
    }
}

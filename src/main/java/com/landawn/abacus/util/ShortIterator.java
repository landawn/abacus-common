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
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortSupplier;
import com.landawn.abacus.util.stream.ShortStream;

/**
 * A specialized iterator for primitive {@code short} values that extends {@link ImmutableIterator}.
 * This class provides various factory methods and operations for creating and manipulating
 * iterators over {@code short} values without the overhead of boxing/unboxing.
 *
 * <p>The iterator is immutable, meaning elements cannot be removed during iteration.
 * It provides specialized methods like {@code nextShort()} to avoid boxing overhead,
 * and various transformation methods like {@code skip()}, {@code limit()}, and {@code filter()}.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * short[] array = {1, 2, 3, 4, 5};
 * ShortIterator iter = ShortIterator.of(array);
 * while (iter.hasNext()) {
 *     short value = iter.nextShort();
 *     System.out.println(value);
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
public abstract class ShortIterator extends ImmutableIterator<Short> {

    /**
     * Protected constructor for subclasses.
     */
    protected ShortIterator() {
    }

    /**
     * An empty ShortIterator instance that has no elements.
     * This constant can be used to represent an empty iteration without creating new objects.
     */
    public static final ShortIterator EMPTY = new ShortIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public short nextShort() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     * Returns an empty {@code ShortIterator} with no elements.
     *
     * <p>The returned iterator's {@code hasNext()} will always return {@code false},
     * and calling {@code nextShort()} will always throw a {@code NoSuchElementException}.</p>
     *
     * <p>This method always returns the same singleton instance for efficiency.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator empty = ShortIterator.empty();
     * assert !empty.hasNext();
     * }</pre>
     *
     * @return an empty {@code ShortIterator}
     */
    @SuppressWarnings("SameReturnValue")
    public static ShortIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     * Creates a {@code ShortIterator} from the specified short array.
     *
     * <p>If the array is {@code null} or empty, returns an empty iterator.
     * The iterator will iterate over all elements in the array from start to end.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] values = {1, 2, 3};
     * ShortIterator iter = ShortIterator.of(values);
     * }</pre>
     *
     * @param a the {@code short} array (may be {@code null})
     * @return a new {@code ShortIterator} over the array elements, or an empty iterator if the array is {@code null} or empty
     * @see #of(short[], int, int)
     */
    public static ShortIterator of(final short... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a {@code ShortIterator} from a subsequence of the specified short array.
     *
     * <p>The iterator will iterate over elements from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive). If {@code fromIndex} equals {@code toIndex}, an empty
     * iterator is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] values = {1, 2, 3, 4, 5};
     * ShortIterator iter = ShortIterator.of(values, 1, 4);   // iterates over 2, 3, 4
     * }</pre>
     *
     * @param a the {@code short} array (may be {@code null})
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a new {@code ShortIterator} over the specified range, or an empty iterator if the array is {@code null} or {@code fromIndex == toIndex}
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > a.length}, or {@code fromIndex > toIndex}
     */
    public static ShortIterator of(final short[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        }

        return new ShortIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public short nextShort() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }

            @Override
            public short[] toArray() {
                final short[] ret = N.copyOfRange(a, cursor, toIndex);
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

            @Override
            public ShortList toList() {
                final ShortList ret = ShortList.of(N.copyOfRange(a, cursor, toIndex));
                cursor = toIndex; // Mark as exhausted
                return ret;
            }
        };
    }

    /**
     * Creates a ShortIterator that is initialized lazily using the provided Supplier.
     * The actual iterator is not created until the first method call on the returned iterator.
     * This is useful for deferring expensive iterator creation until it's actually needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator iter = ShortIterator.defer(() -> ShortIterator.of(computeExpensiveArray()));
     * // The expensive computation is not performed until iter.hasNext() or iter.nextShort() is called
     * }</pre>
     *
     * @param iteratorSupplier a {@link Supplier} that provides the {@code ShortIterator} when needed; must not be {@code null}
     * @return a lazily initialized {@code ShortIterator}
     * @throws IllegalArgumentException if {@code iteratorSupplier} is {@code null}
     */
    public static ShortIterator defer(final Supplier<? extends ShortIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new ShortIterator() {
            private ShortIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public short nextShort() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextShort();
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
     * Creates an infinite ShortIterator that generates values using the provided supplier.
     * The iterator will continuously return values from the supplier and never return {@code false} from hasNext().
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator randomShorts = ShortIterator.generate(() -> (short)(Math.random() * 100));
     * // Use with limit to avoid infinite iteration
     * randomShorts.limit(10).toList();
     * }</pre>
     *
     * @param supplier the supplier function that generates {@code short} values; must not be {@code null}
     * @return an infinite {@code ShortIterator}
     * @throws IllegalArgumentException if {@code supplier} is {@code null}
     * @see #generate(BooleanSupplier, ShortSupplier)
     */
    public static ShortIterator generate(final ShortSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier);

        return new ShortIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public short nextShort() {
                return supplier.getAsShort();
            }
        };
    }

    /**
     * Creates a ShortIterator that generates values using the provided supplier while the hasNext condition is {@code true}.
     * This allows for creating finite iterators with custom termination conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] counter = {0};
     * ShortIterator iter = ShortIterator.generate(
     *     () -> counter[0] < 5,
     *     () -> (short)(counter[0]++)
     * );
     * // Will generate: 0, 1, 2, 3, 4
     * }</pre>
     *
     * @param hasNext a {@link BooleanSupplier} that determines if there are more elements; must not be {@code null}
     * @param supplier the supplier function that generates {@code short} values; must not be {@code null}
     * @return a conditional {@code ShortIterator}
     * @throws IllegalArgumentException if {@code hasNext} or {@code supplier} is {@code null}
     * @see #generate(ShortSupplier)
     */
    public static ShortIterator generate(final BooleanSupplier hasNext, final ShortSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new ShortIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public short nextShort() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return supplier.getAsShort();
            }
        };
    }

    /**
     * Returns the next element in the iteration as a boxed Short.
     * This method is deprecated in favor of {@link #nextShort()} to avoid unnecessary boxing.
     *
     * @return the next {@code short} element as a boxed {@link Short} object
     * @throws NoSuchElementException if the iteration has no more elements
     * @deprecated use {@link #nextShort()} instead to avoid boxing overhead
     */
    @Deprecated
    @Override
    public Short next() {
        return nextShort();
    }

    /**
     * Returns the next short value in the iteration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator iter = ShortIterator.of((short)1, (short)2, (short)3);
     * short first = iter.nextShort();    // 1
     * short second = iter.nextShort();   // 2
     * }</pre>
     *
     * @return the next {@code short} value
     * @throws NoSuchElementException if the iteration has no more elements
     */
    public abstract short nextShort();

    /**
     * Returns a new ShortIterator that skips the first n elements.
     * If n is greater than the number of remaining elements, all elements are skipped.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator iter = ShortIterator.of(new short[] {1, 2, 3, 4, 5});
     * ShortIterator skipped = iter.skip(2);   // Will iterate over 3, 4, 5
     * }</pre>
     *
     * @param n the number of elements to skip
     * @return a new {@code ShortIterator} that skips the first {@code n} elements;
     *         returns this iterator unchanged if {@code n <= 0}
     * @throws IllegalArgumentException if {@code n} is negative
     * @see #limit(long)
     */
    public ShortIterator skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final ShortIterator iter = this;

        return new ShortIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public short nextShort() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextShort();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextShort();
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns a new ShortIterator that limits the number of elements to iterate over.
     * The returned iterator will iterate over at most count elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator iter = ShortIterator.generate(() -> (short)1);
     * ShortIterator limited = iter.limit(3);   // Will only return three 1s
     * }</pre>
     *
     * @param count the maximum number of elements to iterate
     * @return a new {@code ShortIterator} limited to {@code count} elements;
     *         returns an empty iterator if {@code count == 0}
     * @throws IllegalArgumentException if {@code count} is negative
     * @see #skip(long)
     */
    public ShortIterator limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return ShortIterator.EMPTY;
        }

        final ShortIterator iter = this;

        return new ShortIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public short nextShort() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.nextShort();
            }
        };
    }

    /**
     * Returns a new ShortIterator that only includes elements matching the given predicate.
     * Elements that don't satisfy the predicate are skipped.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator iter = ShortIterator.of(new short[] {1, 2, 3, 4, 5});
     * ShortIterator evens = iter.filter(x -> x % 2 == 0);   // Will iterate over 2, 4
     * }</pre>
     *
     * @param predicate the predicate to test each element; must not be {@code null}
     * @return a new filtered {@code ShortIterator}
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
     */
    public ShortIterator filter(final ShortPredicate predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate);

        final ShortIterator iter = this;

        return new ShortIterator() {
            private boolean hasNext = false;
            private short next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        next = iter.nextShort();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public short nextShort() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        };
    }

    /**
     * Converts the remaining elements to a short array.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] array = ShortIterator.of((short)1, (short)2, (short)3, (short)4, (short)5).toArray();
     * // array = [1, 2, 3, 4, 5]
     *
     * // Empty iterator returns empty array
     * short[] empty = ShortIterator.empty().toArray();   // empty.length == 0
     * }</pre>
     *
     * @return a {@code short} array containing all remaining elements; an empty array if there are none
     * @see #toList()
     */
    @SuppressWarnings("deprecation")
    public short[] toArray() {
        return toList().trimToSize().internalArray();
    }

    /**
     * Converts the remaining elements to a ShortList.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty ShortList.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortList list = ShortIterator.of((short)1, (short)2, (short)3, (short)4, (short)5).toList();
     * // list contains [1, 2, 3, 4, 5]
     *
     * // Empty iterator returns empty list
     * ShortList empty = ShortIterator.empty().toList();   // empty.size() == 0
     * }</pre>
     *
     * @return a {@link ShortList} containing all remaining elements; an empty list if there are none
     * @see #toArray()
     */
    public ShortList toList() {
        final ShortList list = new ShortList();

        while (hasNext()) {
            list.add(nextShort());
        }

        return list;
    }

    /**
     * Creates a ShortStream from the remaining elements in this iterator.
     * This provides access to stream operations like map, filter, reduce, etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double average = ShortIterator.of(new short[] {1, 2, 3, 4, 5})
     *     .stream()
     *     .average()
     *     .orElse(0.0);
     * }</pre>
     *
     * @return a {@link ShortStream} of the remaining elements
     */
    public ShortStream stream() {
        return ShortStream.of(this);
    }

    /**
     * Returns an iterator of IndexedShort objects pairing each element with its index.
     * Indexing starts from 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator iter = ShortIterator.of(new short[] {10, 20, 30});
     * ObjIterator<IndexedShort> indexed = iter.indexed();
     * // Will produce: IndexedShort(10, 0), IndexedShort(20, 1), IndexedShort(30, 2)
     * }</pre>
     *
     * @return an {@link ObjIterator} of {@link IndexedShort} objects
     * @see #indexed(long)
     */
    @Beta
    public ObjIterator<IndexedShort> indexed() {
        return indexed(0);
    }

    /**
     * Returns an iterator of IndexedShort objects pairing each element with its index.
     * Indexing starts from the specified startIndex.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator iter = ShortIterator.of(new short[] {10, 20, 30});
     * ObjIterator<IndexedShort> indexed = iter.indexed(100);
     * // Will produce: IndexedShort(10, 100), IndexedShort(20, 101), IndexedShort(30, 102)
     * }</pre>
     *
     * @param startIndex the starting index value
     * @return an {@link ObjIterator} of {@link IndexedShort} objects
     * @throws IllegalArgumentException if {@code startIndex} is negative
     * @see #indexed()
     */
    @Beta
    public ObjIterator<IndexedShort> indexed(final long startIndex) {
        N.checkArgNotNegative(startIndex, cs.startIndex);

        final ShortIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedShort next() {
                return IndexedShort.of(iter.nextShort(), idx++);
            }
        };
    }

    /**
     * Performs the given action for each remaining element using Java's Consumer interface.
     * This method is deprecated because it causes boxing of primitive shorts.
     *
     * @param action the action to perform on each element
     * @deprecated use {@link #foreachRemaining(Throwables.ShortConsumer)} instead to avoid boxing overhead
     */
    @Deprecated
    @Override
    public void forEachRemaining(final java.util.function.Consumer<? super Short> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining short element.
     * This method avoids boxing overhead by using a specialized ShortConsumer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator iter = ShortIterator.of(new short[] {1, 2, 3});
     * iter.foreachRemaining(value -> System.out.println(value));
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each element
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachRemaining(final Throwables.ShortConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextShort());
        }
    }

    /**
     * Performs the given action for each remaining element along with its index.
     * The index starts from 0 and increments for each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortIterator iter = ShortIterator.of(new short[] {10, 20, 30});
     * iter.foreachIndexed((index, value) ->
     *     System.out.println("Element at " + index + " is " + value));
     * // Prints:
     * // Element at 0 is 10
     * // Element at 1 is 20
     * // Element at 2 is 30
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each element with its index; must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws IllegalStateException if the iterator has more than {@link Integer#MAX_VALUE} elements (index overflow)
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntShortConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            if (idx < 0) {
                throw new IllegalStateException("Index overflow: iterator has more than Integer.MAX_VALUE elements");
            }

            action.accept(idx, nextShort());

            idx++;
        }
    }
}

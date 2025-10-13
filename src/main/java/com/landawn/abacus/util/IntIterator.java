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
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.stream.IntStream;

/**
 * A specialized iterator for primitive int values, providing better performance than Iterator<Integer>
 * by avoiding boxing/unboxing overhead. This abstract class provides various utility methods for
 * creating, transforming, and consuming int iterators.
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
 * while (iter.hasNext()) {
 *     System.out.println(iter.nextInt());
 * }
 * 
 * // Using functional operations
 * int sum = IntIterator.of(1, 2, 3, 4, 5)
 *     .filter(x -> x % 2 == 0)
 *     .stream()
 *     .sum();
 * }</pre>
 *
 * @see ObjIterator
 * @see BiIterator
 * @see TriIterator
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 * @since 0.8
 */
@SuppressWarnings({ "java:S6548" })
public abstract class IntIterator extends ImmutableIterator<Integer> {

    /**
     * An empty IntIterator that has no elements.
     */
    public static final IntIterator EMPTY = new IntIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public int nextInt() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     * Returns an empty IntIterator instance.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * IntIterator empty = IntIterator.empty();
     * empty.hasNext(); // returns false
     * }</pre>
     * 
     * @return an empty IntIterator
     */
    @SuppressWarnings("SameReturnValue")
    public static IntIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     * Creates an IntIterator from an int array.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
     * IntList list = iter.toList(); // [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param a the int array to create the iterator from
     * @return an IntIterator over the array elements
     */
    public static IntIterator of(final int... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates an IntIterator from a specified range of an int array.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * int[] array = {1, 2, 3, 4, 5};
     * IntIterator iter = IntIterator.of(array, 1, 4); // iterates over 2, 3, 4
     * }</pre>
     *
     * @param a the int array
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return an IntIterator over the specified range
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds
     */
    public static IntIterator of(final int[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return EMPTY;
        }

        return new IntIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public int nextInt() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }

            @Override
            public int[] toArray() {
                final int[] ret = N.copyOfRange(a, cursor, toIndex);
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

            @Override
            public IntList toList() {
                final IntList ret = IntList.of(N.copyOfRange(a, cursor, toIndex));
                cursor = toIndex; // Mark as exhausted
                return ret;
            }
        };
    }

    /**
     * Creates a deferred IntIterator that is initialized lazily using the provided Supplier.
     * The Supplier is called only when the first method of the iterator is invoked.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * IntIterator iter = IntIterator.defer(() -> IntIterator.of(computeExpensiveArray()));
     * // Array computation happens only when iter is first used
     * if (condition) {
     *     iter.hasNext(); // Triggers computation
     * }
     * }</pre>
     *
     * @param iteratorSupplier a Supplier that provides the IntIterator when needed
     * @return a lazily initialized IntIterator
     * @throws IllegalArgumentException if iteratorSupplier is null
     */
    public static IntIterator defer(final Supplier<? extends IntIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new IntIterator() {
            private IntIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextInt();
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
     * Creates an infinite IntIterator that generates values using the provided IntSupplier.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Random rand = new Random();
     * IntIterator randomInts = IntIterator.generate(rand::nextInt);
     * // Generates infinite random integers
     * 
     * IntIterator constants = IntIterator.generate(() -> 42);
     * // Generates infinite stream of 42
     * }</pre>
     *
     * @param supplier the IntSupplier to generate values
     * @return an infinite IntIterator
     * @throws IllegalArgumentException if supplier is null
     */
    public static IntIterator generate(final IntSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier);

        return new IntIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public int nextInt() {
                return supplier.getAsInt();
            }
        };
    }

    /**
     * Creates an IntIterator that generates values while a condition is true.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * int[] counter = {0};
     * IntIterator iter = IntIterator.generate(
     *     () -> counter[0] < 5,
     *     () -> counter[0]++
     * );
     * // Generates: 0, 1, 2, 3, 4
     * }</pre>
     *
     * @param hasNext the BooleanSupplier that determines if more elements exist
     * @param supplier the IntSupplier to generate values
     * @return an IntIterator that generates values conditionally
     * @throws IllegalArgumentException if hasNext or supplier is null
     */
    public static IntIterator generate(final BooleanSupplier hasNext, final IntSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new IntIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public int nextInt() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return supplier.getAsInt();
            }
        };
    }

    /**
     * Returns the next element as an Integer (boxed).
     * 
     * @return the next int value as Integer
     * @deprecated use {@code nextInt()} instead to avoid boxing
     */
    @Deprecated
    @Override
    public Integer next() {
        return nextInt();
    }

    /**
     * Returns the next int value in the iteration.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * IntIterator iter = IntIterator.of(1, 2, 3);
     * int first = iter.nextInt(); // 1
     * int second = iter.nextInt(); // 2
     * }</pre>
     * 
     * @return the next int value
     * @throws NoSuchElementException if the iteration has no more elements
     */
    public abstract int nextInt();

    /**
     * Skips the specified number of elements in this iterator.
     *
     * <p>If n is 0, returns this iterator unchanged. If n is greater than or equal to
     * the number of remaining elements, all elements will be skipped and the returned
     * iterator will be empty.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
     * IntIterator skipped = iter.skip(2); // Skips 1 and 2
     * skipped.nextInt(); // Returns 3
     *
     * // Skip more than available
     * IntIterator iter2 = IntIterator.of(1, 2);
     * iter2.skip(10).hasNext(); // Returns false
     * }</pre>
     *
     * @param n the number of elements to skip, must be non-negative
     * @return this iterator if n is 0, otherwise a new IntIterator with n elements skipped
     * @throws IllegalArgumentException if n is negative
     */
    public IntIterator skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return this;
        }

        final IntIterator iter = this;

        return new IntIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextInt();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextInt();
                }

                skipped = true;
            }
        };
    }

    /**
     * Limits this iterator to return at most the specified number of elements.
     *
     * <p>If count is 0, returns an empty iterator. If count is greater than or equal to
     * the number of remaining elements, all remaining elements will be included.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Limit infinite iterator
     * IntIterator iter = IntIterator.generate(() -> 1);
     * IntIterator limited = iter.limit(3);
     * limited.toArray(); // Returns [1, 1, 1]
     *
     * // Limit to 0 returns empty
     * IntIterator.of(1, 2, 3).limit(0).hasNext(); // Returns false
     * }</pre>
     *
     * @param count the maximum number of elements to return, must be non-negative
     * @return an empty iterator if count is 0, otherwise a new IntIterator limited to count elements
     * @throws IllegalArgumentException if count is negative
     */
    public IntIterator limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return IntIterator.EMPTY;
        }

        final IntIterator iter = this;

        return new IntIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public int nextInt() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.nextInt();
            }
        };
    }

    /**
     * Filters elements based on the given predicate.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
     * IntIterator evens = iter.filter(x -> x % 2 == 0);
     * evens.toArray(); // Returns [2, 4]
     * }</pre>
     *
     * @param predicate the predicate to test elements
     * @return a new IntIterator containing only elements that match the predicate
     * @throws IllegalArgumentException if predicate is null
     */
    public IntIterator filter(final IntPredicate predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate);

        final IntIterator iter = this;

        return new IntIterator() {
            private boolean hasNext = false;
            private int next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        next = iter.nextInt();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public int nextInt() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        };
    }

    /**
     * Returns the first element as an OptionalInt, or empty if no elements exist.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * OptionalInt first = IntIterator.of(1, 2, 3).first(); // OptionalInt.of(1)
     * OptionalInt empty = IntIterator.empty().first(); // OptionalInt.empty()
     * }</pre>
     * 
     * @return an OptionalInt containing the first element, or empty
     */
    public OptionalInt first() {
        if (hasNext()) {
            return OptionalInt.of(nextInt());
        } else {
            return OptionalInt.empty();
        }
    }

    /**
     * Returns the last element as an OptionalInt, or empty if no elements exist.
     * This method consumes the entire iterator.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * OptionalInt last = IntIterator.of(1, 2, 3).last(); // OptionalInt.of(3)
     * OptionalInt empty = IntIterator.empty().last(); // OptionalInt.empty()
     * }</pre>
     * 
     * @return an OptionalInt containing the last element, or empty
     */
    public OptionalInt last() {
        if (hasNext()) {
            int next = nextInt();

            while (hasNext()) {
                next = nextInt();
            }

            return OptionalInt.of(next);
        } else {
            return OptionalInt.empty();
        }
    }

    /**
     * Converts the remaining elements to an int array.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty array.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * int[] array = IntIterator.of(1, 2, 3, 4, 5).toArray();
     * // array = [1, 2, 3, 4, 5]
     *
     * // Empty iterator returns empty array
     * int[] empty = IntIterator.empty().toArray(); // empty.length == 0
     * }</pre>
     *
     * @return an int array containing all remaining elements
     */
    @SuppressWarnings("deprecation")
    public int[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     * Converts the remaining elements to an IntList.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty IntList.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * IntList list = IntIterator.of(1, 2, 3).toList();
     * // list contains [1, 2, 3]
     *
     * // Empty iterator returns empty list
     * IntList empty = IntIterator.empty().toList(); // empty.size() == 0
     * }</pre>
     *
     * @return an IntList containing all remaining elements
     */
    public IntList toList() {
        final IntList list = new IntList();

        while (hasNext()) {
            list.add(nextInt());
        }

        return list;
    }

    /**
     * Converts this iterator to an IntStream for further processing.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * int sum = IntIterator.of(1, 2, 3, 4, 5)
     *     .stream()
     *     .filter(x -> x > 2)
     *     .sum(); // sum = 12
     * }</pre>
     * 
     * @return an IntStream backed by this iterator
     */
    public IntStream stream() {
        return IntStream.of(this);
    }

    /**
     * Returns an iterator that provides indexed access to elements.
     * Each element is paired with its index starting from 0.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * IntIterator.of(10, 20, 30)
     *     .indexed()
     *     .foreachRemaining(idx -> System.out.println(idx.index() + ": " + idx.value()));
     * // Prints: 0: 10, 1: 20, 2: 30
     * }</pre>
     * 
     * @return an ObjIterator of IndexedInt elements
     */
    @Beta
    public ObjIterator<IndexedInt> indexed() {
        return indexed(0);
    }

    /**
     * Returns an iterator that provides indexed access to elements.
     * Each element is paired with its index starting from the specified value.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * IntIterator.of(10, 20, 30)
     *     .indexed(100)
     *     .foreachRemaining(idx -> System.out.println(idx.index() + ": " + idx.value()));
     * // Prints: 100: 10, 101: 20, 102: 30
     * }</pre>
     *
     * @param startIndex the starting index value
     * @return an ObjIterator of IndexedInt elements
     * @throws IllegalArgumentException if startIndex is negative
     */
    @Beta
    public ObjIterator<IndexedInt> indexed(final long startIndex) {
        N.checkArgNotNegative(startIndex, cs.startIndex);

        final IntIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedInt next() {
                return IndexedInt.of(iter.nextInt(), idx++);
            }
        };
    }

    /**
     * Performs the given action for each remaining element.
     *
     * @param action the action to perform 
     * @deprecated use {@link #foreachRemaining(Throwables.IntConsumer)} instead to avoid boxing
     */
    @Deprecated
    @Override
    public void forEachRemaining(final java.util.function.Consumer<? super Integer> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining element.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false).</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * IntIterator.of(1, 2, 3, 4, 5)
     *     .foreachRemaining(System.out::println);
     * // Prints each number on a new line
     *
     * // With custom action
     * IntList result = new IntList();
     * IntIterator.of(1, 2, 3).foreachRemaining(result::add);
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each element, must not be null
     * @throws IllegalArgumentException if action is null
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachRemaining(final Throwables.IntConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextInt());
        }
    }

    /**
     * Performs the given action for each remaining element along with its index.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). The index starts at 0 for the first
     * remaining element.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * IntIterator.of(10, 20, 30)
     *     .foreachIndexed((index, value) -> System.out.println(index + ": " + value));
     * // Prints: 0: 10, 1: 20, 2: 30
     *
     * // Process with index
     * IntIterator.of(5, 15, 25)
     *     .foreachIndexed((i, v) -> System.out.println("Element " + i + " is " + v));
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each index-value pair, must not be null
     * @throws IllegalArgumentException if action is null
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntIntConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            if (idx < 0) {
                throw new IllegalStateException("Index overflow: iterator has more than Integer.MAX_VALUE elements");
            }
            action.accept(idx++, nextInt());
        }
    }
}

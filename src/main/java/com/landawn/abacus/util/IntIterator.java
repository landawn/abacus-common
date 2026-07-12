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
import com.landawn.abacus.util.stream.IntStream;

/**
 * A specialized iterator for primitive int values, providing better performance than Iterator&lt;Integer&gt;
 * by avoiding boxing/unboxing overhead. This abstract class provides various utility methods for
 * creating, transforming, and consuming int iterators.
 *
 * <p><b>Usage Examples:</b></p>
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
 */
@SuppressWarnings({ "java:S6548" })
public abstract class IntIterator extends ImmutableIterator<Integer> {

    /**
     * Constructs a new {@code IntIterator}.
     * Intended for use by subclasses only.
     */
    protected IntIterator() {
    }

    /**
     * A singleton empty {@code IntIterator} instance that contains no elements.
     * This iterator's {@code hasNext()} always returns {@code false}, and {@code nextInt()}
     * always throws {@link NoSuchElementException}.
     *
     * @see #empty()
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
     * Returns an empty {@code IntIterator} with no elements.
     *
     * <p>The returned iterator's {@code hasNext()} will always return {@code false},
     * and calling {@code nextInt()} will always throw a {@code NoSuchElementException}.</p>
     *
     * <p>This method always returns the same singleton instance for efficiency.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator empty = IntIterator.empty();
     * empty.hasNext();   // returns false
     * }</pre>
     *
     * @return an empty {@code IntIterator}
     */
    @SuppressWarnings("SameReturnValue")
    public static IntIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     * Creates an {@code IntIterator} from the specified int array.
     *
     * <p>If the array is {@code null} or empty, returns an empty iterator.
     * The iterator will iterate over all elements in the array from start to end.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
     * IntList list = iter.toList();   // returns [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param a the int array (may be {@code null})
     * @return a new {@code IntIterator} over the array elements, or an empty iterator if the array is {@code null} or empty
     */
    public static IntIterator of(final int... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates an {@code IntIterator} from a subsequence of the specified int array.
     *
     * <p>The iterator will iterate over elements from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive). If {@code fromIndex} equals {@code toIndex}, an empty
     * iterator is returned. A {@code null} array is treated as length 0 for range validation,
     * so only {@code fromIndex == toIndex == 0} is valid.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] array = {1, 2, 3, 4, 5};
     * IntIterator iter = IntIterator.of(array, 1, 4);   // iterates over 2, 3, 4
     * }</pre>
     *
     * @param a the int array (may be {@code null})
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a new {@code IntIterator} over the specified range, or an empty iterator if the validated range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > (a == null ? 0 : a.length)}, or {@code fromIndex > toIndex}
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator iter = IntIterator.defer(() -> IntIterator.of(computeExpensiveArray()));
     * // Array computation happens only when iter is first used
     * if (condition) {
     *     iter.hasNext();   // Triggers computation
     * }
     * }</pre>
     *
     * @param iteratorSupplier a {@code Supplier} that provides the {@code IntIterator} when needed
     * @return a lazily initialized {@code IntIterator}
     * @throws IllegalArgumentException if {@code iteratorSupplier} is {@code null}
     * @throws IllegalStateException if the supplier returns {@code null} when invoked
     */
    public static IntIterator defer(final Supplier<? extends IntIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new IntIterator() {
            private IntIterator iter = null;
            private volatile boolean isInitialized = false;
            private Throwable initializationFailure = null;

            @Override
            public boolean hasNext() {
                init();

                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                init();

                return iter.nextInt();
            }

            private void init() {
                if (!isInitialized) {
                    synchronized (this) {
                        if (!isInitialized) {
                            try {
                                iter = iteratorSupplier.get();

                                if (iter == null) {
                                    throw new IllegalStateException("Iterator supplier returned null");
                                }
                            } catch (RuntimeException | Error e) {
                                initializationFailure = e;
                            } finally {
                                isInitialized = true;
                            }
                        }
                    }
                }

                if (initializationFailure instanceof RuntimeException) {
                    throw (RuntimeException) initializationFailure;
                } else if (initializationFailure != null) {
                    throw (Error) initializationFailure;
                }
            }
        };
    }

    /**
     * Creates an infinite IntIterator that generates values using the provided IntSupplier.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Random rand = new Random();
     * IntIterator randomInts = IntIterator.generate(rand::nextInt);
     * // Generates infinite random integers
     *
     * IntIterator constants = IntIterator.generate(() -> 42);
     * // Generates infinite stream of 42
     * }</pre>
     *
     * @param supplier the {@code IntSupplier} used to generate each value
     * @return an infinite {@code IntIterator} whose {@code hasNext()} always returns {@code true}
     * @throws IllegalArgumentException if {@code supplier} is {@code null}
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
     * Creates an IntIterator that generates values while a condition is {@code true}.
     * The {@code hasNext} supplier is called at most once per element; its result is cached
     * until the next call to {@code nextInt()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] counter = {0};
     * IntIterator iter = IntIterator.generate(
     *     () -> counter[0] < 5,
     *     () -> counter[0]++
     * );
     * // Generates: 0, 1, 2, 3, 4
     * }</pre>
     *
     * @param hasNext the {@code BooleanSupplier} that determines whether more elements exist
     * @param supplier the {@code IntSupplier} used to generate each value
     * @return an {@code IntIterator} that generates values while {@code hasNext} returns {@code true}
     * @throws IllegalArgumentException if {@code hasNext} or {@code supplier} is {@code null}
     */
    public static IntIterator generate(final BooleanSupplier hasNext, final IntSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new IntIterator() {
            private boolean hasNextCached = false;
            private boolean hasNextValue = false;

            @Override
            public boolean hasNext() {
                if (!hasNextCached) {
                    hasNextValue = hasNext.getAsBoolean();
                    hasNextCached = true;
                }
                return hasNextValue;
            }

            @Override
            public int nextInt() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextCached = false;
                return supplier.getAsInt();
            }
        };
    }

    /**
     * Returns the next element in the iteration as a boxed Integer.
     *
     * <p><b>Note:</b> This method is deprecated because it causes unnecessary boxing overhead.
     * Use {@link #nextInt()} instead for better performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator iter = IntIterator.of(1, 2);
     * Integer boxed = iter.next();    // returns 1 (boxed) — avoid this
     * int primitive = iter.nextInt(); // returns 2 — prefer this
     * }</pre>
     *
     * @return the next element in the iteration as a boxed Integer
     * @throws NoSuchElementException if the iteration has no more elements
     * @deprecated Use {@link #nextInt()} to avoid boxing overhead
     */
    @Deprecated
    @Override
    public Integer next() {
        return nextInt();
    }

    /**
     * Returns the next int value in the iteration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator iter = IntIterator.of(1, 2, 3);
     * int first = iter.nextInt();    // returns 1
     * int second = iter.nextInt();   // returns 2
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
     * IntIterator skipped = iter.skip(2);   // Skips 1 and 2
     * skipped.nextInt();                    // returns 3
     *
     * // Skip more than available
     * IntIterator iter2 = IntIterator.of(1, 2);
     * iter2.skip(10).hasNext();   // returns false
     * }</pre>
     *
     * @param n the number of elements to skip; must be non-negative
     * @return this iterator unchanged if {@code n == 0}, otherwise a new {@code IntIterator}
     *         with the first {@code n} elements skipped
     * @throws IllegalArgumentException if {@code n} is negative
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Limit infinite iterator
     * IntIterator iter = IntIterator.generate(() -> 1);
     * IntIterator limited = iter.limit(3);
     * limited.toArray();   // returns [1, 1, 1]
     *
     * // Limit to 0 returns empty
     * IntIterator.of(1, 2, 3).limit(0).hasNext();   // returns false
     * }</pre>
     *
     * @param count the maximum number of elements to return; must be non-negative
     * @return an empty iterator if {@code count == 0}, otherwise a new {@code IntIterator}
     *         limited to at most {@code count} elements
     * @throws IllegalArgumentException if {@code count} is negative
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
     * Returns a new IntIterator containing only elements that satisfy the given predicate.
     * Elements that do not match the predicate are skipped.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator iter = IntIterator.of(1, 2, 3, 4, 5);
     * IntIterator evens = iter.filter(x -> x % 2 == 0);
     * evens.toArray();   // returns [2, 4]
     * }</pre>
     *
     * @param predicate the predicate to test each element; must not be {@code null}
     * @return a new {@code IntIterator} containing only elements that match the predicate
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
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
     * Converts the remaining elements to an int array.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty ({@code hasNext()} returns {@code false}). If the iterator is already
     * empty, returns an empty array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] array = IntIterator.of(1, 2, 3, 4, 5).toArray();
     * // array = [1, 2, 3, 4, 5]
     *
     * // Empty iterator returns empty array
     * int[] empty = IntIterator.empty().toArray();   // returns empty.length == 0
     * }</pre>
     *
     * @return an {@code int} array containing all remaining elements; an empty array if there are none
     */
    @SuppressWarnings("deprecation")
    public int[] toArray() {
        return toList().trimToSize().internalArray();
    }

    /**
     * Converts the remaining elements to an {@link IntList}.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty ({@code hasNext()} returns {@code false}). If the iterator is already
     * empty, returns an empty {@link IntList}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntList list = IntIterator.of(1, 2, 3).toList();
     * // list contains [1, 2, 3]
     *
     * // Empty iterator returns empty list
     * IntList empty = IntIterator.empty().toList();   // returns empty.size() == 0
     * }</pre>
     *
     * @return an {@link IntList} containing all remaining elements; an empty list if there are none
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int sum = IntIterator.of(1, 2, 3, 4, 5)
     *     .stream()
     *     .filter(x -> x > 2)
     *     .sum();   // returns 12
     * }</pre>
     *
     * @return an IntStream backed by this iterator
     */
    public IntStream stream() {
        return IntStream.of(this);
    }

    /**
     * Returns an iterator that pairs each remaining element with its zero-based index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator.of(10, 20, 30)
     *     .indexed()
     *     .foreachRemaining(idx -> System.out.println(idx.index() + ": " + idx.value()));
     * // Prints: 0: 10, 1: 20, 2: 30
     * }</pre>
     *
     * @return an {@link ObjIterator} of {@link IndexedInt} elements with indices starting at 0
     */
    @Beta
    public ObjIterator<IndexedInt> indexed() {
        return indexed(0);
    }

    /**
     * Returns an iterator that pairs each remaining element with its index,
     * with indices starting from the specified {@code startIndex}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator.of(10, 20, 30)
     *     .indexed(100)
     *     .foreachRemaining(idx -> System.out.println(idx.index() + ": " + idx.value()));
     * // Prints: 100: 10, 101: 20, 102: 30
     * }</pre>
     *
     * @param startIndex the starting index value; must be non-negative
     * @return an {@link ObjIterator} of {@link IndexedInt} elements with indices starting at {@code startIndex}
     * @throws IllegalArgumentException if {@code startIndex} is negative
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
     * Performs the given action for each remaining element, boxing each {@code int} to an {@link Integer}.
     *
     * <p><b>Note:</b> This method is deprecated because it causes unnecessary boxing overhead.
     * Use {@link #foreachRemaining(Throwables.IntConsumer)} instead for better performance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntIterator iter = IntIterator.of(1, 2, 3);
     * iter.forEachRemaining(value -> System.out.println(value));   // Boxes each int — avoid this
     * }</pre>
     *
     * @param action the action to perform on each remaining element
     * @deprecated use {@link #foreachRemaining(Throwables.IntConsumer)} instead to avoid boxing overhead
     */
    @Deprecated
    @Override
    public void forEachRemaining(final java.util.function.Consumer<? super Integer> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining element without boxing overhead.
     * This method consumes the iterator; after it returns, {@code hasNext()} will return {@code false}.
     *
     * <p><b>Usage Examples:</b></p>
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
     * @param action the action to perform on each element; must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
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
     * <p><b>Usage Examples:</b></p>
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
     * @throws IllegalStateException if the iterator contains more than {@link Integer#MAX_VALUE} elements,
     *         causing the index to overflow
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntIntConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            if (idx < 0) {
                throw new IllegalStateException("Index overflow: iterator has more than Integer.MAX_VALUE elements");
            }

            action.accept(idx, nextInt());

            idx++;
        }
    }
}

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
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.function.BooleanPredicate;
import com.landawn.abacus.util.stream.Stream;

/**
 * A specialized Iterator for primitive boolean values.
 * This abstract class provides an efficient iteration mechanism for boolean values
 * without the overhead of boxing/unboxing.
 * 
 * <p>This iterator is immutable and provides various utility methods for transformation,
 * filtering, and conversion operations. It extends ImmutableIterator to ensure that
 * the remove() operation is not supported.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * boolean[] values = {true, false, true, true, false};
 * BooleanIterator iter = BooleanIterator.of(values);
 * 
 * while (iter.hasNext()) {
 *     boolean value = iter.nextBoolean();
 *     System.out.println(value);
 * }
 * }</pre>
 *
 * @see ObjIterator
 * @see BiIterator
 * @see TriIterator
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 * @since 1.0
 */
@SuppressWarnings({ "java:S6548" })
public abstract class BooleanIterator extends ImmutableIterator<Boolean> {

    /** An empty BooleanIterator instance that always returns {@code false} for hasNext() */
    public static final BooleanIterator EMPTY = new BooleanIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public boolean nextBoolean() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     * Returns an empty BooleanIterator.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.empty();
     * System.out.println(iter.hasNext()); // false
     * }</pre>
     *
     * @return an empty BooleanIterator instance
     */
    @SuppressWarnings("SameReturnValue")
    public static BooleanIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     * Creates a BooleanIterator from a boolean array.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true);
     * // Iterates over three boolean values
     * }</pre>
     *
     * @param a the boolean array to create an iterator from
     * @return a BooleanIterator over the array elements
     */
    public static BooleanIterator of(final boolean... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a BooleanIterator from a portion of a boolean array.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * boolean[] array = {true, false, true, false, true};
     * BooleanIterator iter = BooleanIterator.of(array, 1, 4);
     * // Iterates over elements at indices 1, 2, and 3 (false, true, false)
     * }</pre>
     *
     * @param a the boolean array
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a BooleanIterator over the specified range
     * @throws IndexOutOfBoundsException if the indices are out of range
     */
    public static BooleanIterator of(final boolean[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (a == null || fromIndex == toIndex) {
            return EMPTY;
        }

        return new BooleanIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public boolean nextBoolean() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }

            @Override
            public boolean[] toArray() {
                final boolean[] ret = N.copyOfRange(a, cursor, toIndex);
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

            @Override
            public BooleanList toList() {
                final BooleanList ret = BooleanList.of(N.copyOfRange(a, cursor, toIndex));
                cursor = toIndex; // Mark as exhausted
                return ret;
            }
        };
    }

    /**
     * Returns a BooleanIterator instance created lazily using the provided Supplier.
     * The Supplier is responsible for producing the BooleanIterator instance when the first method in the returned {@code BooleanIterator} is called.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.defer(() -> BooleanIterator.of(true, false, true));
     * // Iterator is not created until first use
     * }</pre>
     *
     * @param iteratorSupplier A Supplier that provides the BooleanIterator when needed
     * @return A BooleanIterator that is initialized on the first call to hasNext() or nextBoolean()
     * @throws IllegalArgumentException if iteratorSupplier is {@code null}
     */
    public static BooleanIterator defer(final Supplier<? extends BooleanIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new BooleanIterator() {
            private BooleanIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public boolean nextBoolean() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextBoolean();
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
     * Returns an infinite {@code BooleanIterator} that generates values using the provided supplier.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * Random random = new Random();
     * BooleanIterator randomBools = BooleanIterator.generate(random::nextBoolean);
     * // Generates infinite random boolean values
     * }</pre>
     *
     * @param supplier the supplier function to generate boolean values
     * @return an infinite BooleanIterator
     * @throws IllegalArgumentException if supplier is null
     */
    public static BooleanIterator generate(final BooleanSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier);

        return new BooleanIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public boolean nextBoolean() {
                return supplier.getAsBoolean();
            }
        };
    }

    /**
     * Returns a BooleanIterator that generates values while a condition is true.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * AtomicInteger counter = new AtomicInteger(0);
     * BooleanIterator iter = BooleanIterator.generate(
     *     () -> counter.get() < 5,
     *     () -> counter.incrementAndGet() % 2 == 0
     * );
     * // Generates 5 boolean values based on even/odd counter
     * }</pre>
     *
     * @param hasNext the supplier that determines if there are more elements
     * @param supplier the supplier function to generate boolean values
     * @return a conditional BooleanIterator
     * @throws IllegalArgumentException if hasNext or supplier is null
     */
    public static BooleanIterator generate(final BooleanSupplier hasNext, final BooleanSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new BooleanIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public boolean nextBoolean() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return supplier.getAsBoolean();
            }
        };
    }

    /**
     * Returns the next element as a Boolean object.
     * 
     * @return the next boolean value as a Boolean object
     * @throws NoSuchElementException if the iteration has no more elements
     * @deprecated use {@code nextBoolean()} instead
     */
    @Deprecated
    @Override
    public Boolean next() {
        return nextBoolean();
    }

    /**
     * Returns the next boolean value in the iteration.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false);
     * boolean first = iter.nextBoolean(); // true
     * boolean second = iter.nextBoolean(); // false
     * }</pre>
     *
     * @return the next boolean value
     * @throws NoSuchElementException if the iteration has no more elements
     */
    public abstract boolean nextBoolean();

    /**
     * Skips the specified number of elements in the iteration.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true, false);
     * BooleanIterator skipped = iter.skip(2);
     * boolean value = skipped.nextBoolean(); // true (third element)
     * }</pre>
     *
     * @param n the number of elements to skip
     * @return a new BooleanIterator with elements skipped
     * @throws IllegalArgumentException if n is negative
     */
    public BooleanIterator skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final BooleanIterator iter = this;

        return new BooleanIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public boolean nextBoolean() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextBoolean();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextBoolean();
                }

                skipped = true;
            }
        };
    }

    /**
     * Limits the number of elements returned by this iterator.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true, false);
     * BooleanIterator limited = iter.limit(2);
     * // Will only return the first two elements
     * }</pre>
     *
     * @param count the maximum number of elements to return
     * @return a new BooleanIterator limited to the specified count
     * @throws IllegalArgumentException if count is negative
     */
    public BooleanIterator limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return BooleanIterator.EMPTY;
        }

        final BooleanIterator iter = this;

        return new BooleanIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public boolean nextBoolean() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.nextBoolean();
            }
        };
    }

    /**
     * Filters elements based on the provided predicate.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true, false);
     * BooleanIterator filtered = iter.filter(b -> b == true);
     * // Will only return true values
     * }</pre>
     *
     * @param predicate the predicate to test elements
     * @return a new BooleanIterator containing only elements that match the predicate
     * @throws IllegalArgumentException if predicate is null
     */
    public BooleanIterator filter(final BooleanPredicate predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate);

        final BooleanIterator iter = this;

        return new BooleanIterator() {
            private boolean hasNext = false;
            private boolean next = false;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        next = iter.nextBoolean();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public boolean nextBoolean() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        };
    }

    /**
     * Returns the first element as an OptionalBoolean, or empty if no elements exist.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false);
     * OptionalBoolean first = iter.first(); // OptionalBoolean.of(true)
     * 
     * BooleanIterator empty = BooleanIterator.empty();
     * OptionalBoolean none = empty.first(); // OptionalBoolean.empty()
     * }</pre>
     *
     * @return an OptionalBoolean containing the first element, or empty
     */
    public OptionalBoolean first() {
        if (hasNext()) {
            return OptionalBoolean.of(nextBoolean());
        } else {
            return OptionalBoolean.empty();
        }
    }

    /**
     * Returns the last element as an OptionalBoolean, or empty if no elements exist.
     * 
     * <p>Note: This method consumes the entire iterator.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true);
     * OptionalBoolean last = iter.last(); // OptionalBoolean.of(true)
     * }</pre>
     *
     * @return an OptionalBoolean containing the last element, or empty
     */
    public OptionalBoolean last() {
        if (hasNext()) {
            boolean next = nextBoolean();

            while (hasNext()) {
                next = nextBoolean();
            }

            return OptionalBoolean.of(next);
        } else {
            return OptionalBoolean.empty();
        }
    }

    /**
     * Converts the remaining elements to a boolean array.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true);
     * boolean[] array = iter.toArray();
     * // array contains [true, false, true]
     * }</pre>
     *
     * @return a boolean array containing all remaining elements
     */
    @SuppressWarnings("deprecation")
    public boolean[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     * Converts the remaining elements to a BooleanList.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true);
     * BooleanList list = iter.toList();
     * // list contains [true, false, true]
     * }</pre>
     *
     * @return a BooleanList containing all remaining elements
     */
    public BooleanList toList() {
        final BooleanList list = new BooleanList();

        while (hasNext()) {
            list.add(nextBoolean());
        }

        return list;
    }

    /**
     * Converts this iterator to a Stream of Boolean values.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true);
     * long trueCount = iter.stream()
     *     .filter(b -> b)
     *     .count(); // Returns 2
     * }</pre>
     *
     * @return a Stream of Boolean values
     */
    public Stream<Boolean> stream() {
        return Stream.of(this);
    }

    /**
     * Returns an iterator of IndexedBoolean elements with indices starting from 0.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false);
     * ObjIterator<IndexedBoolean> indexed = iter.indexed();
     * // Returns IndexedBoolean{index=0, value=true}, IndexedBoolean{index=1, value=false}
     * }</pre>
     *
     * @return an ObjIterator of IndexedBoolean elements
     */
    @Beta
    public ObjIterator<IndexedBoolean> indexed() {
        return indexed(0);
    }

    /**
     * Returns an iterator of IndexedBoolean elements with indices starting from the specified value.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false);
     * ObjIterator<IndexedBoolean> indexed = iter.indexed(10);
     * // Returns IndexedBoolean{index=10, value=true}, IndexedBoolean{index=11, value=false}
     * }</pre>
     *
     * @param startIndex the starting index value
     * @return an ObjIterator of IndexedBoolean elements
     * @throws IllegalArgumentException if startIndex is negative
     */
    @Beta
    public ObjIterator<IndexedBoolean> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final BooleanIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedBoolean next() {
                return IndexedBoolean.of(iter.nextBoolean(), idx++);
            }
        };
    }

    /**
     * For each remaining element, performs the given action.
     *
     * @param action the action to be performed for each element
     * @deprecated use {@link #foreachRemaining(Throwables.BooleanConsumer)} instead to avoid boxing
     */
    @Deprecated
    @Override
    public void forEachRemaining(final java.util.function.Consumer<? super Boolean> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining element.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true);
     * iter.foreachRemaining(System.out::println);
     * // Prints each boolean value
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each element
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachRemaining(final Throwables.BooleanConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextBoolean());
        }
    }

    /**
     * Performs the given action for each remaining element, providing the element index.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true);
     * iter.foreachIndexed((index, value) -> 
     *     System.out.println("Index " + index + ": " + value)
     * );
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each element with its index
     * @throws IllegalArgumentException if action is null
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntBooleanConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextBoolean());
        }
    }
}

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
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.DoubleStream;

/**
 * A specialized iterator for primitive double values that extends ImmutableIterator.
 * This class provides efficient iteration over double values without boxing overhead.
 * 
 * <p>DoubleIterator is particularly useful when working with large collections of primitive
 * double values where performance is critical. It provides various utility methods for
 * transforming, filtering, and processing double values.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create iterator from array
 * double[] values = {1.5, 2.5, 3.5, 4.5};
 * DoubleIterator iter = DoubleIterator.of(values);
 * 
 * // Iterate through values
 * while (iter.hasNext()) {
 *     double value = iter.nextDouble();
 *     System.out.println(value);
 * }
 * 
 * // Use with filtering
 * DoubleIterator filtered = DoubleIterator.of(values)
 *     .filter(d -> d > 2.0);
 * 
 * // Convert to stream
 * DoubleStream stream = DoubleIterator.of(values).stream();
 * }</pre>
 *
 * @see ObjIterator
 * @see BiIterator
 * @see TriIterator
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 */
@SuppressWarnings({ "java:S6548" })
public abstract class DoubleIterator extends ImmutableIterator<Double> {

    /** An empty DoubleIterator instance that contains no elements */
    public static final DoubleIterator EMPTY = new DoubleIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public double nextDouble() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     * Returns an empty DoubleIterator instance.
     *
     * @return an empty DoubleIterator
     */
    @SuppressWarnings("SameReturnValue")
    public static DoubleIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     * Creates a DoubleIterator from a double array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] values = {1.1, 2.2, 3.3};
     * DoubleIterator iter = DoubleIterator.of(values);
     * }</pre>
     *
     * @param a the double array to create an iterator from
     * @return a new DoubleIterator over the array elements
     */
    public static DoubleIterator of(final double... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a DoubleIterator from a portion of a double array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] values = {1.0, 2.0, 3.0, 4.0, 5.0};
     * DoubleIterator iter = DoubleIterator.of(values, 1, 4); // iterates over 2.0, 3.0, 4.0
     * }</pre>
     *
     * @param a the double array
     * @param fromIndex the start index (inclusive)
     * @param toIndex the end index (exclusive)
     * @return a new DoubleIterator over the specified range
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds
     */
    public static DoubleIterator of(final double[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (a == null || fromIndex == toIndex) {
            return EMPTY;
        }

        return new DoubleIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public double nextDouble() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }

            @Override
            public double[] toArray() {
                final double[] ret = N.copyOfRange(a, cursor, toIndex);
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

            @Override
            public DoubleList toList() {
                final DoubleList ret = DoubleList.of(N.copyOfRange(a, cursor, toIndex));
                cursor = toIndex; // Mark as exhausted
                return ret;
            }
        };
    }

    /**
     * Creates a DoubleIterator that is initialized lazily using the provided Supplier.
     * The Supplier is invoked only when the first method on the returned iterator is called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator iter = DoubleIterator.defer(() -> {
     *     // Expensive initialization
     *     double[] data = loadLargeDataset();
     *     return DoubleIterator.of(data);
     * });
     * }</pre>
     *
     * @param iteratorSupplier a Supplier that provides the DoubleIterator when needed
     * @return a lazily initialized DoubleIterator
     * @throws IllegalArgumentException if iteratorSupplier is null
     * @throws IllegalStateException if the supplier returns null when invoked
     */
    public static DoubleIterator defer(final Supplier<? extends DoubleIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new DoubleIterator() {
            private DoubleIterator iter = null;
            private volatile boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public double nextDouble() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextDouble();
            }

            private synchronized void init() {
                if (!isInitialized) {
                    isInitialized = true;
                    iter = iteratorSupplier.get();
                    if (iter == null) {
                        throw new IllegalStateException("Iterator supplier returned null");
                    }
                }
            }
        };
    }

    /**
     * Creates an infinite DoubleIterator that generates values using the provided supplier.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate random doubles infinitely
     * DoubleIterator randomIter = DoubleIterator.generate(Math::random);
     * 
     * // Must use limit() to avoid infinite iteration
     * randomIter.limit(5).foreachRemaining(System.out::println);
     * }</pre>
     *
     * @param supplier the DoubleSupplier that generates values
     * @return an infinite DoubleIterator
     * @throws IllegalArgumentException if supplier is null
     */
    public static DoubleIterator generate(final DoubleSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier, cs.supplier);

        return new DoubleIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public double nextDouble() {
                return supplier.getAsDouble();
            }
        };
    }

    /**
     * Creates a DoubleIterator that generates values while the hasNext condition is true.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int count = 0;
     * DoubleIterator iter = DoubleIterator.generate(
     *     () -> count < 10,
     *     () -> count++ * 1.5
     * );
     * }</pre>
     *
     * @param hasNext the BooleanSupplier that determines if more elements exist
     * @param supplier the DoubleSupplier that generates values
     * @return a conditional DoubleIterator
     * @throws IllegalArgumentException if hasNext or supplier is null
     */
    public static DoubleIterator generate(final BooleanSupplier hasNext, final DoubleSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new DoubleIterator() {
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
            public double nextDouble() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextCached = false;
                return supplier.getAsDouble();
            }
        };
    }

    /**
     * Returns the next element as a boxed Double.
     * This method is deprecated; use {@link #nextDouble()} instead for better performance.
     *
     * @return the next double value as a Double object
     * @deprecated use {@code nextDouble()} instead to avoid boxing overhead
     */
    @Deprecated
    @Override
    public Double next() {
        return nextDouble();
    }

    /**
     * Returns the next double value in the iteration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator iter = DoubleIterator.of(1.0, 2.0, 3.0);
     * double first = iter.nextDouble(); // 1.0
     * double second = iter.nextDouble(); // 2.0
     * }</pre>
     *
     * @return the next double value
     * @throws NoSuchElementException if the iteration has no more elements
     */
    public abstract double nextDouble();

    /**
     * Skips the specified number of elements in this iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] values = {1.0, 2.0, 3.0, 4.0, 5.0};
     * DoubleIterator iter = DoubleIterator.of(values).skip(2);
     * // iter will start from 3.0
     * }</pre>
     *
     * @param n the number of elements to skip
     * @return a new DoubleIterator with the first n elements skipped
     * @throws IllegalArgumentException if n is negative
     */
    public DoubleIterator skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return this;
        }

        final DoubleIterator iter = this;

        return new DoubleIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public double nextDouble() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextDouble();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextDouble();
                }

                skipped = true;
            }
        };
    }

    /**
     * Limits this iterator to return at most the specified number of elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] values = {1.0, 2.0, 3.0, 4.0, 5.0};
     * DoubleIterator iter = DoubleIterator.of(values).limit(3);
     * // iter will only return 1.0, 2.0, 3.0
     * }</pre>
     *
     * @param count the maximum number of elements to iterate
     * @return a new DoubleIterator limited to the specified count
     * @throws IllegalArgumentException if count is negative
     */
    public DoubleIterator limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return DoubleIterator.EMPTY;
        }

        final DoubleIterator iter = this;

        return new DoubleIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public double nextDouble() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.nextDouble();
            }
        };
    }

    /**
     * Returns a filtered DoubleIterator that only includes elements matching the predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] values = {1.5, 2.5, 3.5, 4.5};
     * DoubleIterator iter = DoubleIterator.of(values)
     *     .filter(d -> d > 2.5);
     * // iter will only return 3.5, 4.5
     * }</pre>
     *
     * @param predicate the predicate to test each element
     * @return a new filtered DoubleIterator
     * @throws IllegalArgumentException if predicate is null
     */
    public DoubleIterator filter(final DoublePredicate predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate);

        final DoubleIterator iter = this;

        return new DoubleIterator() {
            private boolean hasNextValue = false;
            private double next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNextValue) {
                    while (iter.hasNext()) {
                        next = iter.nextDouble();

                        if (predicate.test(next)) {
                            hasNextValue = true;
                            break;
                        }
                    }
                }

                return hasNextValue;
            }

            @Override
            public double nextDouble() {
                if (!hasNextValue && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextValue = false;

                return next;
            }
        };
    }

    /**
     * Returns the first element in this iterator wrapped in an OptionalDouble.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalDouble first = DoubleIterator.of(1.5, 2.5, 3.5).first();
     * // first.get() returns 1.5
     * }</pre>
     *
     * @return an OptionalDouble containing the first element, or empty if no elements
     */
    public OptionalDouble first() {
        if (hasNext()) {
            return OptionalDouble.of(nextDouble());
        } else {
            return OptionalDouble.empty();
        }
    }

    /**
     * Returns the last element in this iterator wrapped in an OptionalDouble.
     * This method consumes the entire iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalDouble last = DoubleIterator.of(1.5, 2.5, 3.5).last();
     * // last.get() returns 3.5
     * }</pre>
     *
     * @return an OptionalDouble containing the last element, or empty if no elements
     */
    public OptionalDouble last() {
        if (hasNext()) {
            double next = nextDouble();

            while (hasNext()) {
                next = nextDouble();
            }

            return OptionalDouble.of(next);
        } else {
            return OptionalDouble.empty();
        }
    }

    /**
     * Converts the remaining elements to a double array.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] array = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0).toArray();
     * // array = [1.0, 2.0, 3.0, 4.0, 5.0]
     *
     * // Empty iterator returns empty array
     * double[] empty = DoubleIterator.empty().toArray(); // empty.length == 0
     * }</pre>
     *
     * @return a double array containing all remaining elements
     */
    @SuppressWarnings("deprecation")
    public double[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     * Converts the remaining elements to a DoubleList.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty DoubleList.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleList list = DoubleIterator.of(1.0, 2.0, 3.0, 4.0, 5.0).toList();
     * // list contains [1.0, 2.0, 3.0, 4.0, 5.0]
     *
     * // Empty iterator returns empty list
     * DoubleList empty = DoubleIterator.empty().toList(); // empty.size() == 0
     * }</pre>
     *
     * @return a DoubleList containing all remaining elements
     */
    public DoubleList toList() {
        final DoubleList list = new DoubleList();

        while (hasNext()) {
            list.add(nextDouble());
        }

        return list;
    }

    /**
     * Converts this iterator to a DoubleStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double sum = DoubleIterator.of(1.5, 2.5, 3.5)
     *     .stream()
     *     .sum();
     * // sum is 7.5
     * }</pre>
     *
     * @return a DoubleStream backed by this iterator
     */
    public DoubleStream stream() {
        return DoubleStream.of(this);
    }

    /**
     * Returns an iterator of IndexedDouble objects that pairs each element with its index.
     * The indexing starts from 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator.of(10.5, 20.5, 30.5)
     *     .indexed()
     *     .foreachRemaining(indexed -> 
     *         System.out.println("Index: " + indexed.index() + ", Value: " + indexed.value())
     *     );
     * }</pre>
     *
     * @return an ObjIterator of IndexedDouble objects
     */
    @Beta
    public ObjIterator<IndexedDouble> indexed() {
        return indexed(0);
    }

    /**
     * Returns an iterator of IndexedDouble objects that pairs each element with its index,
     * starting from the specified start index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator.of(10.5, 20.5, 30.5)
     *     .indexed(100)
     *     .foreachRemaining(indexed -> 
     *         System.out.println("Index: " + indexed.index() + ", Value: " + indexed.value())
     *     );
     * // Prints indices 100, 101, 102 with corresponding values
     * }</pre>
     *
     * @param startIndex the starting index value
     * @return an ObjIterator of IndexedDouble objects
     * @throws IllegalArgumentException if startIndex is negative
     */
    @Beta
    public ObjIterator<IndexedDouble> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final DoubleIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedDouble next() {
                return IndexedDouble.of(iter.nextDouble(), idx++);
            }
        };
    }

    /**
     * Performs the given action for each remaining element.
     * This method is deprecated; use type-specific methods instead.
     *
     * @param action the action to perform on each element 
     * @deprecated use {@link #foreachRemaining(Throwables.DoubleConsumer)} instead to avoid boxing
     */
    @Deprecated
    @Override
    public void forEachRemaining(final java.util.function.Consumer<? super Double> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining double element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator.of(1.5, 2.5, 3.5)
     *     .foreachRemaining(d -> System.out.println(d * 2));
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each element
     * @throws IllegalArgumentException if action is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachRemaining(final Throwables.DoubleConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextDouble());
        }
    }

    /**
     * Performs the given action for each remaining element along with its index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator.of(10.5, 20.5, 30.5)
     *     .foreachIndexed((index, value) ->
     *         System.out.println("Position " + index + ": " + value)
     *     );
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to perform on each element with its index
     * @throws IllegalArgumentException if action is null
     * @throws IllegalStateException if the iterator has more than Integer.MAX_VALUE elements
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntDoubleConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            if (idx < 0) {
                throw new IllegalStateException("Index overflow: iterator has more than Integer.MAX_VALUE elements");
            }
            action.accept(idx++, nextDouble());
        }
    }
}

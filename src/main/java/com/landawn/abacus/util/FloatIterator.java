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
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatSupplier;
import com.landawn.abacus.util.stream.FloatStream;

/**
 * A specialized iterator for primitive float values that avoids the overhead of boxing.
 * This abstract class provides a base implementation for iterating over float values
 * with additional functional operations like filtering, limiting, and skipping.
 * 
 * <p>FloatIterator is immutable and all transformation methods return new iterator instances.
 * It extends {@code ImmutableIterator<Float>} but provides primitive-specific methods
 * to avoid autoboxing.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * FloatIterator iter = FloatIterator.of(1.0f, 2.5f, 3.7f, 4.2f);
 * 
 * // Skip first element and take next two
 * FloatIterator filtered = iter.skip(1).limit(2);
 * 
 * // Convert to array
 * float[] array = filtered.toArray(); // [2.5f, 3.7f]
 * </pre>
 * 
 * @see ObjIterator
 * @see BiIterator
 * @see TriIterator
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 */
@SuppressWarnings({ "java:S6548" })
public abstract class FloatIterator extends ImmutableIterator<Float> {

    /**
     * A singleton empty FloatIterator that has no elements.
     * This instance is immutable and can be safely shared.
     */
    public static final FloatIterator EMPTY = new FloatIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public float nextFloat() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     * Returns an empty FloatIterator singleton instance.
     * 
     * @return an empty FloatIterator
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator empty = FloatIterator.empty();
     * empty.hasNext(); // false
     * </pre>
     */
    @SuppressWarnings("SameReturnValue")
    public static FloatIterator empty() { //NOSONAR
        return EMPTY;
    }

    /**
     * Creates a FloatIterator from a float array.
     * The entire array will be used for iteration.
     *
     * @param a the array to create an iterator from, may be null or empty
     * @return a new FloatIterator over the array elements, or empty iterator if array is null/empty
     * 
     * <p>Example:</p>
     * <pre>
     * float[] values = {1.0f, 2.0f, 3.0f};
     * FloatIterator iter = FloatIterator.of(values);
     * while (iter.hasNext()) {
     *     System.out.println(iter.nextFloat());
     * }
     * </pre>
     */
    public static FloatIterator of(final float... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a FloatIterator from a portion of a float array.
     * Only elements from fromIndex (inclusive) to toIndex (exclusive) will be iterated.
     *
     * @param a the array to create an iterator from
     * @param fromIndex the start index (inclusive)
     * @param toIndex the end index (exclusive)
     * @return a new FloatIterator over the specified array elements
     * @throws IndexOutOfBoundsException if the indices are out of range
     * 
     * <p>Example:</p>
     * <pre>
     * float[] values = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
     * FloatIterator iter = FloatIterator.of(values, 1, 4);
     * // Will iterate over: 2.0f, 3.0f, 4.0f
     * </pre>
     */
    public static FloatIterator of(final float[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (a == null || fromIndex == toIndex) {
            return EMPTY;
        }

        return new FloatIterator() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public float nextFloat() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[cursor++];
            }

            @Override
            public float[] toArray() {
                final float[] ret = N.copyOfRange(a, cursor, toIndex);
                cursor = toIndex; // Mark as exhausted
                return ret;
            }

            @Override
            public FloatList toList() {
                final FloatList ret = FloatList.of(N.copyOfRange(a, cursor, toIndex));
                cursor = toIndex; // Mark as exhausted
                return ret;
            }
        };
    }

    /**
     * Returns a FloatIterator instance created lazily using the provided Supplier.
     * The Supplier is invoked only when the first method is called on the returned iterator.
     * This allows for deferred initialization of the iterator.
     *
     * @param iteratorSupplier A Supplier that provides the FloatIterator when needed
     * @return A FloatIterator that is initialized on first use
     * @throws IllegalArgumentException if iteratorSupplier is null
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator lazy = FloatIterator.defer(() -> {
     *     // Expensive computation
     *     float[] data = loadFloatData();
     *     return FloatIterator.of(data);
     * });
     * // Iterator is not created until first use
     * if (lazy.hasNext()) { // Supplier is invoked here
     *     float value = lazy.nextFloat();
     * }
     * </pre>
     */
    public static FloatIterator defer(final Supplier<? extends FloatIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new FloatIterator() {
            private FloatIterator iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() {
                if (!isInitialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                if (!isInitialized) {
                    init();
                }

                return iter.nextFloat();
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
     * Returns an infinite FloatIterator that generates values using the provided supplier.
     * The supplier is called each time a new value is needed.
     *
     * @param supplier the supplier function to generate float values
     * @return an infinite FloatIterator
     * @throws IllegalArgumentException if supplier is null
     * 
     * <p>Example:</p>
     * <pre>
     * Random random = new Random();
     * FloatIterator randomFloats = FloatIterator.generate(() -> random.nextFloat());
     * 
     * // Take first 5 random values
     * float[] fiveRandoms = randomFloats.limit(5).toArray();
     * </pre>
     */
    public static FloatIterator generate(final FloatSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier);

        return new FloatIterator() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public float nextFloat() {
                return supplier.getAsFloat();
            }
        };
    }

    /**
     * Returns a FloatIterator that generates values while the hasNext condition is true.
     * This allows for creating finite iterators with dynamic termination conditions.
     *
     * @param hasNext a BooleanSupplier that determines if more elements are available
     * @param supplier the supplier function to generate float values
     * @return a FloatIterator that terminates when hasNext returns false
     * @throws IllegalArgumentException if hasNext or supplier is null
     * 
     * <p>Example:</p>
     * <pre>
     * int[] counter = {0};
     * FloatIterator limited = FloatIterator.generate(
     *     () -> counter[0] < 10,
     *     () -> counter[0]++ * 1.5f
     * );
     * // Generates: 0.0, 1.5, 3.0, 4.5, 6.0, 7.5, 9.0, 10.5, 12.0, 13.5
     * </pre>
     */
    public static FloatIterator generate(final BooleanSupplier hasNext, final FloatSupplier supplier) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(supplier);

        return new FloatIterator() {
            @Override
            public boolean hasNext() {
                return hasNext.getAsBoolean();
            }

            @Override
            public float nextFloat() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return supplier.getAsFloat();
            }
        };
    }

    /**
     * Returns the next element as a boxed Float.
     * This method provides compatibility with the Iterator interface but involves boxing overhead.
     *
     * @return the next element as a Float object
     * @throws NoSuchElementException if no more elements are available
     * @deprecated use {@code nextFloat()} instead to avoid boxing
     */
    @Deprecated
    @Override
    public Float next() {
        return nextFloat();
    }

    /**
     * Returns the next float value in the iteration.
     * This is the primary method for retrieving values from the iterator.
     *
     * @return the next float value
     * @throws NoSuchElementException if the iteration has no more elements
     */
    public abstract float nextFloat();

    /**
     * Returns a new FloatIterator that skips the first n elements.
     * If n is greater than the number of remaining elements, an empty iterator is returned.
     * The skipping is performed lazily when the returned iterator is first accessed.
     *
     * @param n the number of elements to skip
     * @return a new FloatIterator with the first n elements skipped
     * @throws IllegalArgumentException if n is negative
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
     * FloatIterator skipped = iter.skip(2);
     * // skipped will iterate over: 3.0f, 4.0f, 5.0f
     * </pre>
     */
    public FloatIterator skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final FloatIterator iter = this;

        return new FloatIterator() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextFloat();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.nextFloat();
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns a new FloatIterator that contains at most the specified number of elements.
     * If the iterator contains fewer elements than the limit, all elements are included.
     *
     * @param count the maximum number of elements to iterate
     * @return a new FloatIterator limited to the specified count
     * @throws IllegalArgumentException if count is negative
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
     * FloatIterator limited = iter.limit(3);
     * // limited will iterate over: 1.0f, 2.0f, 3.0f
     * </pre>
     */
    public FloatIterator limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return FloatIterator.EMPTY;
        }

        final FloatIterator iter = this;

        return new FloatIterator() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public float nextFloat() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.nextFloat();
            }
        };
    }

    /**
     * Returns a new FloatIterator that only includes elements matching the given predicate.
     * The filtering is performed lazily as elements are requested.
     *
     * @param predicate the predicate to test elements
     * @return a new FloatIterator containing only matching elements
     * @throws IllegalArgumentException if predicate is null
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator iter = FloatIterator.of(1.0f, 2.5f, 3.0f, 4.5f, 5.0f);
     * FloatIterator filtered = iter.filter(x -> x > 2.5f);
     * // filtered will iterate over: 3.0f, 4.5f, 5.0f
     * </pre>
     */
    public FloatIterator filter(final FloatPredicate predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate, cs.Predicate);

        final FloatIterator iter = this;

        return new FloatIterator() {
            private boolean hasNext = false;
            private float next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (iter.hasNext()) {
                        next = iter.nextFloat();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public float nextFloat() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        };
    }

    /**
     * Returns the first element wrapped in an OptionalFloat, or empty if no elements exist.
     * This consumes the first element from the iterator if present.
     *
     * @return OptionalFloat containing the first element, or empty if iterator is empty
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator iter = FloatIterator.of(1.5f, 2.5f, 3.5f);
     * OptionalFloat first = iter.first(); // OptionalFloat.of(1.5f)
     * 
     * FloatIterator empty = FloatIterator.empty();
     * OptionalFloat none = empty.first(); // OptionalFloat.empty()
     * </pre>
     */
    public OptionalFloat first() {
        if (hasNext()) {
            return OptionalFloat.of(nextFloat());
        } else {
            return OptionalFloat.empty();
        }
    }

    /**
     * Returns the last element wrapped in an OptionalFloat, or empty if no elements exist.
     * This consumes all elements from the iterator.
     *
     * @return OptionalFloat containing the last element, or empty if iterator is empty
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator iter = FloatIterator.of(1.5f, 2.5f, 3.5f);
     * OptionalFloat last = iter.last(); // OptionalFloat.of(3.5f)
     * </pre>
     */
    public OptionalFloat last() {
        if (hasNext()) {
            float next = nextFloat();

            while (hasNext()) {
                next = nextFloat();
            }

            return OptionalFloat.of(next);
        } else {
            return OptionalFloat.empty();
        }
    }

    /**
     * Converts the remaining elements in this iterator to a float array.
     * This consumes all remaining elements from the iterator.
     *
     * @return a new float array containing all remaining elements
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
     * float[] array = iter.toArray(); // [1.0f, 2.0f, 3.0f]
     * </pre>
     */
    @SuppressWarnings("deprecation")
    public float[] toArray() {
        return toList().trimToSize().array();
    }

    /**
     * Converts the remaining elements in this iterator to a FloatList.
     * This consumes all remaining elements from the iterator.
     *
     * @return a new FloatList containing all remaining elements
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
     * FloatList list = iter.toList();
     * </pre>
     */
    public FloatList toList() {
        final FloatList list = new FloatList();

        while (hasNext()) {
            list.add(nextFloat());
        }

        return list;
    }

    /**
     * Converts this iterator to a FloatStream for use with the Stream API.
     * The stream is lazily populated from this iterator as elements are consumed.
     *
     * @return a new FloatStream backed by this iterator
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f);
     * double sum = iter.stream()
     *     .filter(x -> x > 2.0f)
     *     .sum();
     * </pre>
     */
    public FloatStream stream() {
        return FloatStream.of(this);
    }

    /**
     * Returns an ObjIterator that yields IndexedFloat objects pairing each element with its index.
     * Indexing starts from 0.
     *
     * @return an ObjIterator of IndexedFloat objects
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator iter = FloatIterator.of(1.5f, 2.5f, 3.5f);
     * iter.indexed().forEach(indexed -> 
     *     System.out.println("Index: " + indexed.index() + ", Value: " + indexed.value())
     * );
     * </pre>
     */
    @Beta
    public ObjIterator<IndexedFloat> indexed() {
        return indexed(0);
    }

    /**
     * Returns an ObjIterator that yields IndexedFloat objects pairing each element with its index.
     * Indexing starts from the specified startIndex.
     *
     * @param startIndex the starting index value
     * @return an ObjIterator of IndexedFloat objects
     * @throws IllegalArgumentException if startIndex is negative
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator iter = FloatIterator.of(1.5f, 2.5f, 3.5f);
     * iter.indexed(10).forEach(indexed -> 
     *     System.out.println("Index: " + indexed.index() + ", Value: " + indexed.value())
     * );
     * // Prints indices starting from 10, 11, 12...
     * </pre>
     */
    @Beta
    public ObjIterator<IndexedFloat> indexed(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        final FloatIterator iter = this;

        return new ObjIterator<>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedFloat next() {
                return IndexedFloat.of(iter.nextFloat(), idx++);
            }
        };
    }

    /**
     * Performs the given action for each remaining element until all elements
     * have been processed or the action throws an exception.
     * This method boxes each float value which may impact performance.
     *
     * @param action the action to be performed for each element 
     * @deprecated use {@link #foreachRemaining(Throwables.FloatConsumer)} instead to avoid boxing
     */
    @Deprecated
    @Override
    public void forEachRemaining(final java.util.function.Consumer<? super Float> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining float element.
     * This method uses a primitive float consumer to avoid boxing overhead.
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to be performed for each element
     * @throws E if the action throws an exception
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
     * iter.foreachRemaining(value -> System.out.println(value));
     * </pre>
     */
    public <E extends Exception> void foreachRemaining(final Throwables.FloatConsumer<E> action) throws E { //NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextFloat());
        }
    }

    /**
     * Performs the given action for each remaining element along with its index.
     * The index starts from 0 and increments for each element processed.
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to be performed for each element with its index
     * @throws IllegalArgumentException if action is null
     * @throws E if the action throws an exception
     * 
     * <p>Example:</p>
     * <pre>
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
     * iter.foreachIndexed((index, value) -> 
     *     System.out.println("Index: " + index + ", Value: " + value)
     * );
     * </pre>
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntFloatConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, nextFloat());
        }
    }
}

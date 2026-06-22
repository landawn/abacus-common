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
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * FloatIterator iter = FloatIterator.of(1.0f, 2.5f, 3.7f, 4.2f);
 *
 * // Skip first element and take next two
 * FloatIterator filtered = iter.skip(1).limit(2);
 *
 * // Convert to array
 * float[] array = filtered.toArray();   // returns [2.5f, 3.7f]
 * }</pre>
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
     * Constructs a new {@code FloatIterator}.
     * Intended for use by subclasses only.
     */
    protected FloatIterator() {
    }

    /**
     * A singleton empty {@code FloatIterator} instance that contains no elements.
     * This iterator's {@code hasNext()} always returns {@code false}, and {@code nextFloat()}
     * always throws {@link NoSuchElementException}.
     *
     * @see #empty()
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
     * Returns an empty {@code FloatIterator} with no elements.
     *
     * <p>The returned iterator's {@code hasNext()} will always return {@code false},
     * and calling {@code nextFloat()} will always throw a {@code NoSuchElementException}.</p>
     *
     * <p>This method always returns the same singleton instance for efficiency.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator empty = FloatIterator.empty();
     * empty.hasNext();   // returns false
     * }</pre>
     *
     * @return an empty {@code FloatIterator}
     */
    @SuppressWarnings("SameReturnValue")
    public static FloatIterator empty() { //NOSONAR
        return EMPTY;
    }

    /**
     * Creates a {@code FloatIterator} from the specified float array.
     *
     * <p>If the array is {@code null} or empty, returns an empty iterator.
     * The iterator will iterate over all elements in the array from start to end.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] values = {1.0f, 2.0f, 3.0f};
     * FloatIterator iter = FloatIterator.of(values);
     * while (iter.hasNext()) {
     *     System.out.println(iter.nextFloat());
     * }
     * }</pre>
     *
     * @param a the float array (may be {@code null})
     * @return a new {@code FloatIterator} over the array elements, or an empty iterator if the array is {@code null} or empty
     */
    public static FloatIterator of(final float... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a {@code FloatIterator} from a subsequence of the specified float array.
     *
     * <p>The iterator will iterate over elements from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive). If {@code fromIndex} equals {@code toIndex}, an empty
     * iterator is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] values = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
     * FloatIterator iter = FloatIterator.of(values, 1, 4);
     * // Will iterate over: 2.0f, 3.0f, 4.0f
     * }</pre>
     *
     * @param a the float array (may be {@code null})
     * @param fromIndex the start index (inclusive)
     * @param toIndex the end index (exclusive)
     * @return a new {@code FloatIterator} over the specified range, or an empty iterator if the array is {@code null} or {@code fromIndex == toIndex}
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > a.length}, or {@code fromIndex > toIndex}
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
     * Returns a {@code FloatIterator} whose underlying iterator is created lazily using the provided supplier.
     * The supplier is invoked at most once, on the first call to any method of the returned iterator.
     * This allows for deferred and potentially expensive initialization of the iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator lazy = FloatIterator.defer(() -> {
     *     // Expensive computation
     *     float[] data = loadFloatData();
     *     return FloatIterator.of(data);
     * });
     * // Iterator is not created until first use
     * if (lazy.hasNext()) {
     *     float value = lazy.nextFloat();
     * }
     * }</pre>
     *
     * @param iteratorSupplier a supplier that provides the {@code FloatIterator} when first needed; must not be {@code null}
     * @return a {@code FloatIterator} that is initialized on first use
     * @throws IllegalArgumentException if {@code iteratorSupplier} is {@code null}
     * @throws IllegalStateException if the supplier returns {@code null} when invoked
     */
    public static FloatIterator defer(final Supplier<? extends FloatIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new FloatIterator() {
            private FloatIterator iter = null;
            private volatile boolean isInitialized = false;

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
     * Returns an infinite {@code FloatIterator} that generates values using the provided supplier.
     * The supplier is called each time {@link #nextFloat()} is invoked.
     * The returned iterator's {@link #hasNext()} always returns {@code true}.
     * Use {@link #limit(long)} to bound the number of elements consumed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Random random = new Random();
     * FloatIterator randomFloats = FloatIterator.generate(() -> random.nextFloat());
     *
     * // Take first 5 random values
     * float[] fiveRandoms = randomFloats.limit(5).toArray();
     * }</pre>
     *
     * @param supplier the supplier function to generate float values; must not be {@code null}
     * @return an infinite {@code FloatIterator} backed by the given supplier
     * @throws IllegalArgumentException if {@code supplier} is {@code null}
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
     * Returns a {@code FloatIterator} that generates values while the {@code hasNext} condition
     * returns {@code true}. This allows for creating finite iterators with dynamic termination
     * conditions. {@link #nextFloat()} throws {@link NoSuchElementException} when {@code hasNext}
     * returns {@code false}.
     *
     * <p>Note: The {@code hasNext} supplier may be called multiple times for the same element
     * (once by the user, once internally for validation), so it should be idempotent or
     * designed to handle multiple calls.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] counter = {0};
     * FloatIterator limited = FloatIterator.generate(
     *     () -> counter[0] < 10,
     *     () -> counter[0]++ * 1.5f
     * );
     * // Generates: 0.0, 1.5, 3.0, 4.5, 6.0, 7.5, 9.0, 10.5, 12.0, 13.5
     * }</pre>
     *
     * @param hasNext a {@code BooleanSupplier} that determines if more elements are available; must not be {@code null}
     * @param supplier the supplier function to generate float values; must not be {@code null}
     * @return a {@code FloatIterator} that terminates when {@code hasNext} returns {@code false}
     * @throws IllegalArgumentException if {@code hasNext} or {@code supplier} is {@code null}
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
     * Returns the next element as a boxed {@link Float}.
     * This method provides compatibility with the {@code Iterator<Float>} interface but involves
     * autoboxing overhead. Prefer {@link #nextFloat()} which returns the primitive {@code float} directly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f);
     * Float boxed = iter.next();           // returns 1.0f (boxed) — avoid this
     * float primitive = iter.nextFloat();  // returns 2.0f — prefer this
     * }</pre>
     *
     * @return the next element as a {@link Float} object
     * @throws NoSuchElementException if the iteration has no more elements
     * @deprecated use {@link #nextFloat()} instead to avoid boxing overhead
     */
    @Deprecated
    @Override
    public Float next() {
        return nextFloat();
    }

    /**
     * Returns the next float value in the iteration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
     * float first = iter.nextFloat();    // returns 1.0f
     * float second = iter.nextFloat();   // returns 2.0f
     * }</pre>
     *
     * @return the next float value
     * @throws NoSuchElementException if the iteration has no more elements
     */
    public abstract float nextFloat();

    /**
     * Returns a new {@code FloatIterator} that skips the first {@code n} elements of this iterator.
     * If {@code n} is greater than the number of remaining elements, all elements are skipped
     * and the returned iterator will be empty.
     * The skipping is performed lazily when the returned iterator is first accessed.
     * If {@code n} is zero, this iterator is returned unchanged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
     * FloatIterator skipped = iter.skip(2);
     * // skipped will iterate over: 3.0f, 4.0f, 5.0f
     * }</pre>
     *
     * @param n the number of elements to skip; must be non-negative
     * @return this iterator unchanged if {@code n == 0}, otherwise a new {@code FloatIterator}
     *         with the first {@code n} elements skipped
     * @throws IllegalArgumentException if {@code n} is negative
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
     * Returns a new {@code FloatIterator} that yields at most {@code count} elements from this iterator.
     * If the iterator contains fewer elements than {@code count}, all elements are included.
     * If {@code count} is zero, an empty iterator is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
     * FloatIterator limited = iter.limit(3);
     * // limited will iterate over: 1.0f, 2.0f, 3.0f
     * }</pre>
     *
     * @param count the maximum number of elements to iterate; must be non-negative
     * @return an empty iterator if {@code count == 0}, otherwise a new {@code FloatIterator}
     *         limited to at most {@code count} elements
     * @throws IllegalArgumentException if {@code count} is negative
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
     * Returns a new {@code FloatIterator} that only yields elements for which the given predicate
     * returns {@code true}. The filtering is performed lazily as elements are requested.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter = FloatIterator.of(1.0f, 2.5f, 3.0f, 4.5f, 5.0f);
     * FloatIterator filtered = iter.filter(x -> x > 2.5f);
     * // filtered will iterate over: 3.0f, 4.5f, 5.0f
     * }</pre>
     *
     * @param predicate the predicate used to test elements; must not be {@code null}
     * @return a new {@code FloatIterator} containing only elements that match the predicate
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
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
     * Converts the remaining elements to a float array.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] array = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).toArray();
     * // array = [1.0, 2.0, 3.0, 4.0, 5.0]
     *
     * // Empty iterator returns empty array
     * float[] empty = FloatIterator.empty().toArray();   // returns empty.length == 0
     * }</pre>
     *
     * @return a {@code float} array containing all remaining elements; an empty array if there are none
     */
    @SuppressWarnings("deprecation")
    public float[] toArray() {
        return toList().trimToSize().internalArray();
    }

    /**
     * Converts the remaining elements to a FloatList.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty FloatList.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatList list = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).toList();
     * // list contains [1.0, 2.0, 3.0, 4.0, 5.0]
     *
     * // Empty iterator returns empty list
     * FloatList empty = FloatIterator.empty().toList();   // returns empty.size() == 0
     * }</pre>
     *
     * @return a {@link FloatList} containing all remaining elements; an empty list if there are none
     */
    public FloatList toList() {
        final FloatList list = new FloatList();

        while (hasNext()) {
            list.add(nextFloat());
        }

        return list;
    }

    /**
     * Converts this iterator to a {@link FloatStream} for use with the Stream API.
     * The stream is lazily populated from this iterator as elements are consumed.
     * The iterator must not be used directly after calling this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f);
     * double sum = iter.stream()
     *     .filter(x -> x > 2.0f)
     *     .sum();
     * }</pre>
     *
     * @return a new {@link FloatStream} backed by this iterator
     */
    public FloatStream stream() {
        return FloatStream.of(this);
    }

    /**
     * Returns an {@code ObjIterator} that pairs each remaining element with its zero-based index.
     * Equivalent to {@link #indexed(long) indexed(0)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter = FloatIterator.of(1.5f, 2.5f, 3.5f);
     * iter.indexed().foreachRemaining(indexed ->
     *     System.out.println("Index: " + indexed.index() + ", Value: " + indexed.value())
     * );
     * }</pre>
     *
     * @return an {@code ObjIterator} of {@link IndexedFloat} objects with indices starting at 0
     */
    @Beta
    public ObjIterator<IndexedFloat> indexed() {
        return indexed(0);
    }

    /**
     * Returns an {@code ObjIterator} that pairs each remaining element with its index,
     * starting from the specified {@code startIndex}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter = FloatIterator.of(1.5f, 2.5f, 3.5f);
     * iter.indexed(10).foreachRemaining(indexed ->
     *     System.out.println("Index: " + indexed.index() + ", Value: " + indexed.value())
     * );
     * // Prints indices starting from 10, 11, 12...
     * }</pre>
     *
     * @param startIndex the starting index value; must be non-negative
     * @return an {@code ObjIterator} of {@link IndexedFloat} objects with indices beginning at {@code startIndex}
     * @throws IllegalArgumentException if {@code startIndex} is negative
     */
    @Beta
    public ObjIterator<IndexedFloat> indexed(final long startIndex) {
        N.checkArgNotNegative(startIndex, cs.startIndex);

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
     * For better performance with primitive values, use {@link #foreachRemaining(Throwables.FloatConsumer)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
     * iter.forEachRemaining(value -> System.out.println(value));   // Boxes each float — avoid this
     * }</pre>
     *
     * @param action the action to be performed for each element, must not be null
     * @deprecated use {@link #foreachRemaining(Throwables.FloatConsumer)} instead to avoid boxing
     */
    @Deprecated
    @Override
    public void forEachRemaining(final java.util.function.Consumer<? super Float> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining float element.
     * This method uses a primitive float consumer to avoid boxing overhead,
     * making it more efficient than {@link #forEachRemaining(java.util.function.Consumer)}.
     * The action is applied to each remaining element until all elements have been processed
     * or the action throws an exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
     * iter.foreachRemaining(value -> System.out.println(value));
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to be performed for each element, must not be null
     * @throws IllegalArgumentException if action is null
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachRemaining(final Throwables.FloatConsumer<E> action) throws E { //NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextFloat());
        }
    }

    /**
     * Performs the given action for each remaining element along with its zero-based index.
     * The index starts at 0 and increments by 1 for each element processed.
     * The action receives both the current index (as {@code int}) and the float value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter = FloatIterator.of(1.0f, 2.0f, 3.0f);
     * iter.foreachIndexed((index, value) ->
     *     System.out.println("Index: " + index + ", Value: " + value)
     * );
     * // Output: Index: 0, Value: 1.0
     * //         Index: 1, Value: 2.0
     * //         Index: 2, Value: 3.0
     * }</pre>
     *
     * @param <E> the type of exception the action may throw
     * @param action the action to be performed for each element with its index; must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws IllegalStateException if the iterator yields more than {@link Integer#MAX_VALUE} elements,
     *         causing the index counter to overflow
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntFloatConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            if (idx < 0) {
                throw new IllegalStateException("Index overflow: iterator has more than Integer.MAX_VALUE elements");
            }

            action.accept(idx, nextFloat());

            idx++;
        }
    }
}

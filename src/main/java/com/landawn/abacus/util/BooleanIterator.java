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
 * <p><b>Usage Examples:</b></p>
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
 */
@SuppressWarnings({ "java:S6548" })
public abstract class BooleanIterator extends ImmutableIterator<Boolean> {

    /**
     * Constructs a new {@code BooleanIterator}.
     * Intended for use by subclasses only.
     */
    protected BooleanIterator() {
    }

    /**
     * A singleton empty {@code BooleanIterator} instance that contains no elements.
     * This iterator's {@code hasNext()} always returns {@code false}, and {@code nextBoolean()}
     * always throws {@link NoSuchElementException}.
     *
     * @see #empty()
     */
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
     * Returns an empty {@code BooleanIterator} with no elements.
     *
     * <p>The returned iterator's {@code hasNext()} will always return {@code false},
     * and calling {@code nextBoolean()} will always throw a {@code NoSuchElementException}.</p>
     *
     * <p>This method always returns the same singleton instance for efficiency.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.empty();
     * System.out.println(iter.hasNext());   // returns false
     * }</pre>
     *
     * @return an empty {@code BooleanIterator}
     */
    @SuppressWarnings("SameReturnValue")
    public static BooleanIterator empty() {//NOSONAR
        return EMPTY;
    }

    /**
     * Creates a {@code BooleanIterator} from the specified boolean array.
     *
     * <p>If the array is {@code null} or empty, returns an empty iterator.
     * The iterator will iterate over all elements in the array from start to end.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true);
     * // Iterates over three boolean values
     * }</pre>
     *
     * @param a the boolean array (may be {@code null})
     * @return a new {@code BooleanIterator} over the array elements, or an empty iterator if the array is {@code null} or empty
     */
    public static BooleanIterator of(final boolean... a) {
        return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
    }

    /**
     * Creates a {@code BooleanIterator} from a subsequence of the specified boolean array.
     *
     * <p>The iterator will iterate over elements from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive). If {@code fromIndex} equals {@code toIndex}, an empty
     * iterator is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] array = {true, false, true, false, true};
     * BooleanIterator iter = BooleanIterator.of(array, 1, 4);
     * // Iterates over elements at indices 1, 2, and 3 (false, true, false)
     * }</pre>
     *
     * @param a the boolean array (may be {@code null})
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a new {@code BooleanIterator} over the specified range, or an empty iterator if the array is {@code null} or {@code fromIndex == toIndex}
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > a.length}, or {@code fromIndex > toIndex}
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.defer(() -> BooleanIterator.of(true, false, true));
     * // Iterator is not created until first use
     * }</pre>
     *
     * @param iteratorSupplier a Supplier that provides the {@code BooleanIterator} when needed, must not be {@code null}
     * @return a {@code BooleanIterator} that is initialized on the first call to {@code hasNext()} or {@code nextBoolean()}
     * @throws IllegalArgumentException if {@code iteratorSupplier} is {@code null}
     * @throws IllegalStateException if the supplier returns {@code null} when invoked
     */
    public static BooleanIterator defer(final Supplier<? extends BooleanIterator> iteratorSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

        return new BooleanIterator() {
            private BooleanIterator iter = null;
            private volatile boolean isInitialized = false;

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
     * Returns an infinite {@code BooleanIterator} that generates values using the provided supplier.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Random random = new Random();
     * BooleanIterator randomBools = BooleanIterator.generate(random::nextBoolean);
     * // Generates infinite random boolean values
     * }</pre>
     *
     * @param supplier the supplier function to generate boolean values; must not be {@code null}
     * @return an infinite {@code BooleanIterator} whose {@code hasNext()} always returns {@code true}
     * @throws IllegalArgumentException if {@code supplier} is {@code null}
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
     * Returns a {@code BooleanIterator} that generates values while a condition is {@code true}.
     *
     * <p>Note: The {@code hasNext} supplier may be called multiple times for the same element
     * (once by the user, once internally for validation), so it should be idempotent or
     * designed to handle multiple calls.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicInteger counter = new AtomicInteger(0);
     * BooleanIterator iter = BooleanIterator.generate(
     *     () -> counter.get() < 5,
     *     () -> counter.incrementAndGet() % 2 == 0
     * );
     * // Generates 5 boolean values based on even/odd counter
     * }</pre>
     *
     * @param hasNext the {@link BooleanSupplier} that determines if there are more elements;
     *        must not be {@code null}
     * @param supplier the supplier function to generate boolean values; must not be {@code null}
     * @return a conditional {@code BooleanIterator}
     * @throws IllegalArgumentException if {@code hasNext} or {@code supplier} is {@code null}
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
     * Returns the next boolean value in the iteration as a boxed Boolean.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false);
     * Boolean boxed = iter.next();            // returns true (boxed) — avoid this
     * boolean primitive = iter.nextBoolean(); // returns false — prefer this
     * }</pre>
     *
     * @return the next boolean value as a boxed {@link Boolean} object
     * @throws NoSuchElementException if the iteration has no more elements
     * @deprecated use {@link #nextBoolean()} instead to avoid boxing overhead
     */
    @Deprecated
    @Override
    public Boolean next() {
        return nextBoolean();
    }

    /**
     * Returns the next boolean value in the iteration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false);
     * boolean first = iter.nextBoolean();    // returns true
     * boolean second = iter.nextBoolean();   // returns false
     * }</pre>
     *
     * @return the next boolean value
     * @throws NoSuchElementException if the iteration has no more elements
     */
    public abstract boolean nextBoolean();

    /**
     * Returns a new {@code BooleanIterator} that skips the first {@code n} elements.
     * If {@code n} is greater than the number of remaining elements, all elements are skipped.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true, false);
     * BooleanIterator skipped = iter.skip(2);
     * boolean value = skipped.nextBoolean();   // returns true (third element)
     * }</pre>
     *
     * @param n the number of elements to skip; must be non-negative
     * @return this iterator unchanged if {@code n == 0}, otherwise a new {@code BooleanIterator}
     *         with the first {@code n} elements skipped
     * @throws IllegalArgumentException if {@code n} is negative
     */
    public BooleanIterator skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0) {
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
     * Returns a new {@code BooleanIterator} that will iterate over at most {@code count} elements.
     * If {@code count} is 0, an empty iterator is returned. If {@code count} exceeds the number
     * of remaining elements, all remaining elements are included.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true, false);
     * BooleanIterator limited = iter.limit(2);
     * // Will only return the first two elements: true, false
     * }</pre>
     *
     * @param count the maximum number of elements to return; must be non-negative
     * @return an empty iterator if {@code count == 0}, otherwise a new {@code BooleanIterator}
     *         limited to at most {@code count} elements
     * @throws IllegalArgumentException if {@code count} is negative
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
     * Returns a new {@code BooleanIterator} containing only elements that satisfy the given predicate.
     * Elements that do not match the predicate are skipped.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true, false);
     * BooleanIterator filtered = iter.filter(b -> b);
     * // Will only return true values
     * }</pre>
     *
     * @param predicate the predicate to test each element; must not be {@code null}
     * @return a new {@code BooleanIterator} containing only elements that satisfy the predicate
     * @throws IllegalArgumentException if {@code predicate} is {@code null}
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
     * Converts the remaining elements to a boolean array.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty array.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] array = BooleanIterator.of(true, false, true).toArray();
     * // array = [true, false, true]
     *
     * // Empty iterator returns empty array
     * boolean[] empty = BooleanIterator.empty().toArray();   // returns empty.length == 0
     * }</pre>
     *
     * @return a {@code boolean} array containing all remaining elements; an empty array if there are none
     */
    @SuppressWarnings("deprecation")
    public boolean[] toArray() {
        return toList().trimToSize().internalArray();
    }

    /**
     * Converts the remaining elements to a BooleanList.
     *
     * <p>This method consumes the iterator. After calling this method, the iterator
     * will be empty (hasNext() returns false). If the iterator is already empty,
     * returns an empty BooleanList.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanList list = BooleanIterator.of(true, false, true).toList();
     * // list contains [true, false, true]
     *
     * // Empty iterator returns empty list
     * BooleanList empty = BooleanIterator.empty().toList();   // returns empty.size() == 0
     * }</pre>
     *
     * @return a {@link BooleanList} containing all remaining elements; an empty list if there are none
     */
    public BooleanList toList() {
        final BooleanList list = new BooleanList();

        while (hasNext()) {
            list.add(nextBoolean());
        }

        return list;
    }

    /**
     * Converts this iterator to a boxed {@link Stream}{@code <Boolean>}.
     * Note: unlike the primitive-specific iterator types, {@code BooleanIterator} does not
     * have a corresponding primitive stream type, so this method returns a boxed stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true);
     * long trueCount = iter.stream()
     *     .filter(b -> b)
     *     .count();   // returns 2
     * }</pre>
     *
     * @return a {@link Stream}{@code <Boolean>} backed by this iterator
     */
    public Stream<Boolean> stream() {
        return Stream.of(this);
    }

    /**
     * Returns an iterator that pairs each remaining element with its zero-based index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false);
     * ObjIterator<IndexedBoolean> indexed = iter.indexed();
     * // Produces: IndexedBoolean(index=0, value=true), IndexedBoolean(index=1, value=false)
     * }</pre>
     *
     * @return an {@link ObjIterator} of {@link IndexedBoolean} elements with indices starting at 0
     */
    @Beta
    public ObjIterator<IndexedBoolean> indexed() {
        return indexed(0);
    }

    /**
     * Returns an iterator that pairs each remaining element with its index,
     * with indices starting from the specified {@code startIndex}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false);
     * ObjIterator<IndexedBoolean> indexed = iter.indexed(10);
     * // Produces: IndexedBoolean(index=10, value=true), IndexedBoolean(index=11, value=false)
     * }</pre>
     *
     * @param startIndex the starting index value; must be non-negative
     * @return an {@link ObjIterator} of {@link IndexedBoolean} elements with indices starting at {@code startIndex}
     * @throws IllegalArgumentException if {@code startIndex} is negative
     */
    @Beta
    public ObjIterator<IndexedBoolean> indexed(final long startIndex) {
        N.checkArgNotNegative(startIndex, cs.startIndex);

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
     * Performs the given action for each remaining element, boxing each {@code boolean} to a {@link Boolean}.
     * This method is deprecated because it causes boxing overhead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true);
     * iter.forEachRemaining(value -> System.out.println(value));   // Boxes each boolean — avoid this
     * }</pre>
     *
     * @param action the action to be performed for each element; must not be {@code null}
     * @throws NullPointerException if {@code action} is {@code null}
     * @deprecated use {@link #foreachRemaining(Throwables.BooleanConsumer)} instead to avoid boxing overhead
     */
    @Deprecated
    @Override
    public void forEachRemaining(final java.util.function.Consumer<? super Boolean> action) {
        super.forEachRemaining(action);
    }

    /**
     * Performs the given action for each remaining element without boxing overhead.
     * This method consumes the iterator; after it returns, {@code hasNext()} will return {@code false}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true);
     * iter.foreachRemaining(System.out::println);
     * // Prints each boolean value
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each element; must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachRemaining(final Throwables.BooleanConsumer<E> action) throws E {//NOSONAR
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(nextBoolean());
        }
    }

    /**
     * Performs the given action for each remaining element along with its zero-based index.
     * This method consumes the iterator; after it returns, {@code hasNext()} will return {@code false}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanIterator iter = BooleanIterator.of(true, false, true);
     * iter.foreachIndexed((index, value) ->
     *     System.out.println("Index " + index + ": " + value)
     * );
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to be performed for each (index, value) pair; must not be {@code null}
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws IllegalStateException if the iterator contains more than {@link Integer#MAX_VALUE} elements,
     *         causing the index to overflow
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntBooleanConsumer<E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            if (idx < 0) {
                throw new IllegalStateException("Index overflow: iterator has more than Integer.MAX_VALUE elements");
            }

            action.accept(idx, nextBoolean());

            idx++;
        }
    }
}

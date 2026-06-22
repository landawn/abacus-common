/*
 * Copyright (c) 2024, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import com.landawn.abacus.util.stream.Stream;

/**
 * An abstract list iterator implementation that provides bidirectional iteration
 * with additional functional operations. This class extends {@link ImmutableIterator}
 * and implements {@link ListIterator}, providing methods for forward and backward
 * traversal, as well as index-based operations.
 *
 * <p>ObjListIterator extends the functionality of {@link ObjIterator} by adding
 * bidirectional iteration capabilities. It maintains the current position in the
 * list and allows movement in both directions.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Bidirectional iteration with next() and previous()</li>
 *   <li>Index-aware iteration with nextIndex() and previousIndex()</li>
 *   <li>Lazy evaluation of operations like skip and limit</li>
 *   <li>Convenient factory methods for creating iterators from various sources</li>
 *   <li>Additional utility methods such as {@code toList()}, {@code toArray()}, and {@code stream()}, mirroring those of {@link ObjIterator}</li>
 * </ul>
 *
 * <p>Note: The {@code set()}, {@code add()}, and {@code remove()} operations
 * are not supported and throw {@link UnsupportedOperationException}, preserving
 * immutability.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a list iterator
 * List<String> list = Arrays.asList("a", "b", "c", "d");
 * ObjListIterator<String> iter = ObjListIterator.of(list);
 *
 * // Bidirectional iteration
 * iter.next();       // returns "a"
 * iter.next();       // returns "b"
 * iter.previous();   // returns "b"
 * iter.previous();   // returns "a"
 *
 * // Skip and limit
 * ObjListIterator<String> sliced = ObjListIterator.of(list).skip(1).limit(2);
 * // Iterates over: "b", "c"
 * }</pre>
 *
 * @param <T> the type of elements returned by this iterator
 * @see ImmutableIterator
 * @see ObjIterator
 * @see BiIterator
 * @see TriIterator
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 */
@SuppressWarnings({ "java:S6548" })
public abstract class ObjListIterator<T> extends ImmutableIterator<T> implements ListIterator<T> {

    /**
     * Protected constructor for subclasses.
     */
    protected ObjListIterator() {
    }

    @SuppressWarnings("rawtypes")
    private static final ObjListIterator EMPTY = new ObjListIterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public Object previous() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public int previousIndex() {
            return -1;
        }

        /**
         * Operation not supported.
         *
         * @param e the element argument (not used because this operation is unsupported)
         * @throws UnsupportedOperationException always
         * @deprecated - UnsupportedOperationException
         */
        @Deprecated
        @Override
        public void set(final Object e) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * Operation not supported.
         *
         * @param e the element argument (not used because this operation is unsupported)
         * @throws UnsupportedOperationException always
         * @deprecated - UnsupportedOperationException
         */
        @Deprecated
        @Override
        public void add(final Object e) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    };

    /**
     * Returns an empty {@code ObjListIterator} that has no elements.
     * The returned iterator's {@code hasNext()} and {@code hasPrevious()}
     * always return {@code false}, and {@code next()}/{@code previous()} always
     * throw {@link NoSuchElementException}. A shared singleton instance is
     * returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjListIterator<String> iter = ObjListIterator.empty();
     * boolean fwd = iter.hasNext();       // returns false
     * boolean bwd = iter.hasPrevious();   // returns false
     * iter.next();                        // throws NoSuchElementException
     * }</pre>
     *
     * @param <T> the type of elements (not) returned by the iterator
     * @return an empty {@code ObjListIterator}
     */
    public static <T> ObjListIterator<T> empty() {
        return EMPTY;
    }

    /**
     * Returns an {@code ObjListIterator} containing exactly one element.
     * The cursor starts before the element, so {@code next()} must be called to
     * retrieve it. The value may be {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjListIterator<String> single = ObjListIterator.just("Hello");
     * single.hasNext();       // returns true
     * single.hasPrevious();   // returns false
     * single.next();          // returns "Hello"
     * single.hasPrevious();   // returns true
     * single.hasNext();       // returns false
     * }</pre>
     *
     * @param <T> the type of the element
     * @param val the single element to be returned by the iterator
     * @return an {@code ObjListIterator} containing exactly one element
     */
    public static <T> ObjListIterator<T> just(final T val) {
        return of(Collections.singletonList(val));
    }

    /**
     * Returns an {@code ObjListIterator} over the specified array.
     * The iterator traverses all elements of the array in order, and supports
     * bidirectional iteration. If the array is {@code null} or empty, an empty
     * iterator is returned. {@code null} elements are permitted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] array = {"one", "two", "three"};
     * ObjListIterator<String> iter = ObjListIterator.of(array);
     * }</pre>
     *
     * @param <T> the type of elements in the array
     * @param a the array whose elements are to be iterated
     * @return an {@code ObjListIterator} over the array elements
     */
    @SafeVarargs
    public static <T> ObjListIterator<T> of(final T... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        // Use Arrays.asList instead of List.of: List.of rejects null elements with NPE,
        // which is inconsistent with the array-backed of(T[], int, int) overload that
        // accepts nulls.
        return of(Arrays.asList(a));
    }

    /**
     * Returns an {@code ObjListIterator} over a portion of the specified array.
     * The iterator traverses elements from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive) and supports bidirectional iteration. If the
     * array is empty or the range is empty, an empty iterator is returned. A
     * {@code null} array is treated as length 0 for range validation, so only
     * {@code fromIndex == toIndex == 0} is valid.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] numbers = {1, 2, 3, 4, 5};
     * ObjListIterator<Integer> iter = ObjListIterator.of(numbers, 1, 4);
     * // Iterates over: 2, 3, 4
     * }</pre>
     *
     * @param <T> the type of elements in the array
     * @param a the array whose elements are to be iterated
     * @param fromIndex the index of the first element to iterate (inclusive)
     * @param toIndex the index after the last element to iterate (exclusive)
     * @return an {@code ObjListIterator} over the specified range of array elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0},
     *         {@code toIndex > (a == null ? 0 : a.length)}, or {@code fromIndex > toIndex}
     */
    public static <T> ObjListIterator<T> of(final T[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return empty();
        } else if (fromIndex == 0 && toIndex == a.length) {
            return of(Arrays.asList(a));
        } else {
            return of(Arrays.asList(a).subList(fromIndex, toIndex));
        }
    }

    /**
     * Returns an {@code ObjListIterator} over the elements in the specified
     * {@code List}. If the list is {@code null}, an empty
     * {@code ObjListIterator} is returned. The returned iterator supports
     * bidirectional traversal but not the mutating {@code set()}/{@code add()}/
     * {@code remove()} operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c");
     * ObjListIterator<String> iter = ObjListIterator.of(list);
     * iter.next();       // returns "a"
     * iter.next();       // returns "b"
     * iter.previous();   // returns "b"
     * }</pre>
     *
     * @param <T> the type of elements in the list
     * @param list the {@code List} whose elements are to be iterated
     * @return an {@code ObjListIterator} over the list elements
     */
    public static <T> ObjListIterator<T> of(final List<? extends T> list) {
        return list == null ? ObjListIterator.empty() : of(list.listIterator());
    }

    /**
     * Returns an {@code ObjListIterator} that wraps the specified
     * {@code ListIterator}. If the list iterator is {@code null}, an empty
     * {@code ObjListIterator} is returned. If it is already an
     * {@code ObjListIterator}, it is returned as-is (no wrapping is performed).
     *
     * <p>Note: The returned {@code ObjListIterator} does not support the
     * {@code set()} and {@code add()} operations even if the underlying
     * {@code ListIterator} does; they throw {@link UnsupportedOperationException}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ListIterator<String> listIter = Arrays.asList("a", "b", "c").listIterator();
     * ObjListIterator<String> iter = ObjListIterator.of(listIter);
     * while (iter.hasNext()) {
     *     System.out.println(iter.next());
     * }
     * }</pre>
     *
     * @param <T> the type of elements in the iterator
     * @param iter the {@code ListIterator} to wrap
     * @return an {@code ObjListIterator} wrapping the given list iterator
     */
    public static <T> ObjListIterator<T> of(final ListIterator<? extends T> iter) {
        if (iter == null) {
            return empty();
        } else if (iter instanceof ObjListIterator) {
            return (ObjListIterator<T>) iter;
        }

        return new ObjListIterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                return iter.next();
            }

            @Override
            public boolean hasPrevious() {
                return iter.hasPrevious();
            }

            @Override
            public T previous() {
                return iter.previous();
            }

            @Override
            public int nextIndex() {
                return iter.nextIndex();
            }

            @Override
            public int previousIndex() {
                return iter.previousIndex();
            }

            /**
             * Operation not supported.
             *
             * @param e the element argument (not used because this operation is unsupported)
             * @throws UnsupportedOperationException always
             * @deprecated - UnsupportedOperationException
             */
            @Deprecated
            @Override
            public void set(final T e) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }

            /**
             * Operation not supported.
             *
             * @param e the element argument (not used because this operation is unsupported)
             * @throws UnsupportedOperationException always
             * @deprecated - UnsupportedOperationException
             */
            @Deprecated
            @Override
            public void add(final T e) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns a new {@code ObjListIterator} that skips the first {@code n}
     * elements of this iterator. If {@code n} is greater than or equal to the
     * number of remaining elements, the returned iterator will be empty for
     * forward iteration. The skip is performed lazily on the first call to
     * {@code hasNext()} or {@code next()}. If {@code n} is {@code 0}, this
     * iterator is returned unchanged. Backward iteration delegates to the
     * underlying iterator and reflects whatever elements it has consumed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5));
     * ObjListIterator<Integer> skipped = iter.skip(2);
     * skipped.next();       // returns 3
     * skipped.next();       // returns 4
     * skipped.previous();   // returns 4
     * }</pre>
     *
     * @param n the number of elements to skip
     * @return a new {@code ObjListIterator} that skips the first {@code n} elements
     * @throws IllegalArgumentException if {@code n} is negative
     * @see #limit(long)
     */
    public ObjListIterator<T> skip(final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return this;
        }

        final ObjListIterator<T> iter = this;

        return new ObjListIterator<>() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skip();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.next();
            }

            @Override
            public boolean hasPrevious() {
                return iter.hasPrevious();
            }

            @Override
            public T previous() {
                return iter.previous();
            }

            @Override
            public int nextIndex() {
                return iter.nextIndex();
            }

            @Override
            public int previousIndex() {
                return iter.previousIndex();
            }

            /**
             * Operation not supported.
             *
             * @param e the element argument (not used because this operation is unsupported)
             * @throws UnsupportedOperationException always
             * @deprecated - UnsupportedOperationException
             */
            @Deprecated
            @Override
            public void set(final T e) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }

            /**
             * Operation not supported.
             *
             * @param e the element argument (not used because this operation is unsupported)
             * @throws UnsupportedOperationException always
             * @deprecated - UnsupportedOperationException
             */
            @Deprecated
            @Override
            public void add(final T e) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }

            private void skip() {
                long idx = 0;

                while (idx++ < n && iter.hasNext()) {
                    iter.next();
                }

                skipped = true;
            }
        };
    }

    /**
     * Returns a new {@code ObjListIterator} that yields at most the first
     * {@code count} elements of this iterator in forward iteration. If
     * {@code count} is {@code 0}, an empty iterator is returned. If
     * {@code count} is greater than the number of remaining elements, all
     * remaining elements are returned. Backward iteration delegates to the
     * underlying iterator and is not bounded by {@code count}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c", "d", "e"));
     * ObjListIterator<String> limited = iter.limit(3);
     * // Forward iteration: "a", "b", "c"
     * // Can still go backward after reaching the limit
     * }</pre>
     *
     * @param count the maximum number of elements to return in forward iteration
     * @return a new {@code ObjListIterator} limited to {@code count} elements
     * @throws IllegalArgumentException if {@code count} is negative
     * @see #skip(long)
     */
    public ObjListIterator<T> limit(final long count) throws IllegalArgumentException {
        N.checkArgNotNegative(count, cs.count);

        if (count == 0) {
            return ObjListIterator.empty();
        }

        final ObjListIterator<T> iter = this;

        return new ObjListIterator<>() {
            private long cnt = count;

            @Override
            public boolean hasNext() {
                return cnt > 0 && iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                return iter.next();
            }

            @Override
            public boolean hasPrevious() {
                return iter.hasPrevious();
            }

            @Override
            public T previous() {
                return iter.previous();
            }

            @Override
            public int nextIndex() {
                return iter.nextIndex();
            }

            @Override
            public int previousIndex() {
                return iter.previousIndex();
            }

            /**
             * Operation not supported.
             *
             * @param e the element argument (not used because this operation is unsupported)
             * @throws UnsupportedOperationException always
             * @deprecated - UnsupportedOperationException
             */
            @Deprecated
            @Override
            public void set(final T e) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }

            /**
             * Operation not supported.
             *
             * @param e the element argument (not used because this operation is unsupported)
             * @throws UnsupportedOperationException always
             * @deprecated - UnsupportedOperationException
             */
            @Deprecated
            @Override
            public void add(final T e) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Returns the first {@code non-null} element of this iterator wrapped in an
     * {@link u.Optional}. If no {@code non-null} element is found, an empty
     * {@code Optional} is returned. This is a terminal operation that consumes
     * elements in forward direction until a {@code non-null} element is found
     * (or the iterator is exhausted); the {@code null} elements skipped before
     * it are discarded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList(null, null, "found", "next"));
     * Optional<String> first = iter.firstNonNull();   // returns Optional.of("found")
     * }</pre>
     *
     * @return an {@code Optional} containing the first {@code non-null} element,
     *         or an empty {@code Optional} if none is found
     * @deprecated This method partially consumes the iterator and leaves it in an
     *             intermediate state (elements after the first non-null element remain),
     *             which produces inconsistent results when the iterator is used further.
     */
    @Deprecated
    public u.Optional<T> firstNonNull() {
        T next = null;

        while (hasNext()) {
            next = next();

            if (next != null) {
                return u.Optional.of(next);
            }
        }

        return u.Optional.empty();
    }

    /**
     * Converts the remaining elements of this iterator to an {@code Object[]}.
     * This is a terminal operation that consumes all remaining elements in the
     * forward direction. An empty array is returned if no elements remain.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
     * Object[] array = iter.toArray();   // returns {"a", "b", "c"}
     * }</pre>
     *
     * @return an {@code Object[]} containing all remaining elements
     * @see #toArray(Object[])
     * @see #toList()
     */
    public Object[] toArray() {
        return toArray(N.EMPTY_OBJECT_ARRAY);
    }

    /**
     * Converts the remaining elements of this iterator to an array of the
     * specified type. If the provided array is large enough, it is used
     * directly (following the {@link java.util.Collection#toArray(Object[])}
     * contract); otherwise a new array of the same runtime type is allocated.
     * This is a terminal operation that consumes all remaining elements in the
     * forward direction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
     * String[] array = iter.toArray(new String[0]);   // returns {"a", "b", "c"}
     * }</pre>
     *
     * @param <A> the component type of the array
     * @param a the array into which the elements are stored, if it is big enough;
     *          otherwise a new array of the same runtime type is allocated
     * @return an array containing all remaining elements
     * @throws NullPointerException if {@code a} is {@code null}
     */
    public <A> A[] toArray(final A[] a) {
        return toList().toArray(a);
    }

    /**
     * Converts the remaining elements of this iterator to a {@code List}.
     * This is a terminal operation that consumes all remaining elements in the
     * forward direction. The returned list is a new mutable {@link ArrayList};
     * an empty list is returned if no elements remain.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3));
     * List<Integer> list = iter.toList();   // returns [1, 2, 3]
     * }</pre>
     *
     * @return a mutable {@code List} containing all remaining elements
     * @see #toArray()
     */
    public List<T> toList() {
        final List<T> list = new ArrayList<>();

        while (hasNext()) {
            list.add(next());
        }

        return list;
    }

    /**
     * Returns a {@link Stream} backed by the remaining elements of this
     * iterator. The stream consumes elements lazily from this iterator in the
     * forward direction as it is traversed; this iterator should not be used
     * directly after this method is called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
     * long count = iter.stream()
     *     .filter(s -> s.length() > 0)
     *     .count();   // returns 3
     * }</pre>
     *
     * @return a {@code Stream} of the remaining elements
     */
    public Stream<T> stream() {
        return Stream.of(this);
    }

    /**
     * Performs the given action for each remaining element in forward direction.
     * This is a terminal operation that consumes all remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
     * iter.foreachRemaining(System.out::println);
     * // Prints: a, b, c
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to perform for each remaining element
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws E if the action throws an exception
     * @see #foreachIndexed(Throwables.IntObjConsumer)
     */
    public <E extends Exception> void foreachRemaining(final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        while (hasNext()) {
            action.accept(next());
        }
    }

    /**
     * Performs the given action for each remaining element in forward direction,
     * providing both the element and its index to the action. The index corresponds
     * to the value that would be returned by nextIndex() before calling next().
     * This is a terminal operation that consumes all remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c");
     * ObjListIterator<String> iter = ObjListIterator.of(list);
     * iter.next();
     * iter.foreachIndexed((index, value) ->
     *     System.out.println(index + ": " + value)
     * );
     * // Prints:
     * // 1: b
     * // 2: c
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the action to perform for each element and its index
     * @throws IllegalArgumentException if {@code action} is {@code null}
     * @throws IllegalStateException if {@link #nextIndex()} overflows (more than
     *         {@link Integer#MAX_VALUE} elements)
     * @throws E if the action throws an exception
     * @see #foreachRemaining(Throwables.Consumer)
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntObjConsumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        while (hasNext()) {
            final int idx = nextIndex();

            if (idx < 0) {
                throw new IllegalStateException("Index overflow: iterator has more than Integer.MAX_VALUE elements");
            }

            action.accept(idx, next());
        }
    }
}

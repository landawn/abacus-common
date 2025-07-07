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
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import com.landawn.abacus.util.u.Nullable;
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
 *   <li>Additional utility methods inherited from ObjIterator</li>
 * </ul>
 * 
 * <p>Note: The set() and add() operations are not supported and will throw
 * UnsupportedOperationException, maintaining immutability.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Create a list iterator
 * List<String> list = Arrays.asList("a", "b", "c", "d");
 * ObjListIterator<String> iter = ObjListIterator.of(list);
 * 
 * // Bidirectional iteration
 * iter.next();     // "a"
 * iter.next();     // "b"
 * iter.previous(); // "b"
 * iter.previous(); // "a"
 * 
 * // Skip and limit
 * ObjListIterator<String> sliced = ObjListIterator.of(list).skip(1).limit(2);
 * // Iterates over: "b", "c"
 * }</pre>
 *
 * @param <T> the type of elements returned by this iterator
 * @see ObjIterator
 * @see BiIterator
 * @see TriIterator
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Enumerations
 * @since 1.0
 */
@SuppressWarnings({ "java:S6548" })
public abstract class ObjListIterator<T> extends ImmutableIterator<T> implements ListIterator<T> {

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
     * Returns an empty ObjListIterator that has no elements.
     * The returned iterator's hasNext() and hasPrevious() methods always return false.
     *
     * @param <T> the type of elements (not) returned by the iterator
     * @return an empty ObjListIterator
     */
    public static <T> ObjListIterator<T> empty() {
        return EMPTY;
    }

    /**
     * Returns an ObjListIterator containing a single element.
     * The iterator starts before the element, so next() must be called to access it.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ObjListIterator<String> single = ObjListIterator.just("Hello");
     * single.hasNext();    // true
     * single.hasPrevious(); // false
     * single.next();       // "Hello"
     * single.hasPrevious(); // true
     * single.hasNext();    // false
     * }</pre>
     *
     * @param <T> the type of the element
     * @param val the single element to be returned by the iterator
     * @return an ObjListIterator containing exactly one element
     */
    public static <T> ObjListIterator<T> just(final T val) {
        return of(List.of(val));
    }

    /**
     * Returns an ObjListIterator over the specified array.
     * The iterator will traverse all elements in the array in order.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * String[] array = {"one", "two", "three"};
     * ObjListIterator<String> iter = ObjListIterator.of(array);
     * }</pre>
     *
     * @param <T> the type of elements in the array
     * @param a the array whose elements are to be iterated
     * @return an ObjListIterator over the array elements
     */
    @SafeVarargs
    public static <T> ObjListIterator<T> of(final T... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return of(List.of(a));
    }

    /**
     * Returns an ObjListIterator over a portion of the specified array.
     * The iterator will traverse elements from fromIndex (inclusive) to toIndex (exclusive).
     * 
     * <p>Example:</p>
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
     * @return an ObjListIterator over the specified range of array elements
     * @throws IndexOutOfBoundsException if fromIndex < 0, toIndex > array length, or fromIndex > toIndex
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
     * Returns an ObjListIterator over the elements in the specified List.
     * If the List is null, returns an empty ObjListIterator.
     * The iterator supports all ListIterator operations.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c");
     * ObjListIterator<String> iter = ObjListIterator.of(list);
     * }</pre>
     *
     * @param <T> the type of elements in the list
     * @param list the List whose elements are to be iterated
     * @return an ObjListIterator over the list elements
     */
    public static <T> ObjListIterator<T> of(final List<? extends T> list) {
        return list == null ? ObjListIterator.empty() : of(list.listIterator());
    }

    /**
     * Returns an ObjListIterator that wraps the specified ListIterator.
     * If the ListIterator is null, returns an empty ObjListIterator.
     * If the ListIterator is already an ObjListIterator, returns it as-is.
     * 
     * <p>Note: The returned ObjListIterator does not support set() and add()
     * operations, even if the underlying ListIterator does.</p>
     *
     * @param <T> the type of elements in the iterator
     * @param iter the ListIterator to wrap
     * @return an ObjListIterator wrapping the given ListIterator
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
     * Returns a new ObjListIterator that skips the first n elements.
     * If n is greater than or equal to the number of remaining elements,
     * the returned iterator will be empty for forward iteration.
     * Backward iteration is still supported from the current position.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3, 4, 5));
     * ObjListIterator<Integer> skipped = iter.skip(2);
     * skipped.next(); // 3
     * skipped.next(); // 4
     * skipped.previous(); // 4
     * }</pre>
     *
     * @param n the number of elements to skip
     * @return a new ObjListIterator that skips the first n elements
     * @throws IllegalArgumentException if n is negative
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
     * Returns a new ObjListIterator that yields at most the first count elements.
     * If count is 0, returns an empty iterator.
     * If count is greater than the number of remaining elements,
     * returns all remaining elements.
     * Backward iteration is still fully supported.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c", "d", "e"));
     * ObjListIterator<String> limited = iter.limit(3);
     * // Forward iteration: "a", "b", "c"
     * // Can still go backward after reaching the limit
     * }</pre>
     *
     * @param count the maximum number of elements to return in forward iteration
     * @return a new ObjListIterator limited to count elements
     * @throws IllegalArgumentException if count is negative
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
     * Returns the first element of this iterator wrapped in a Nullable.
     * If the iterator is empty, returns an empty Nullable.
     * This operation consumes the first element if present.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("first", "second"));
     * Nullable<String> first = iter.first(); // Nullable.of("first")
     * }</pre>
     *
     * @return a Nullable containing the first element, or empty if no elements
     */
    public Nullable<T> first() {
        if (hasNext()) {
            return Nullable.of(next());
        } else {
            return Nullable.empty();
        }
    }

    /**
     * Returns the first non-null element of this iterator wrapped in an Optional.
     * If no non-null element is found, returns an empty Optional.
     * This operation may consume multiple elements until a non-null element is found.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList(null, null, "found", "next"));
     * Optional<String> first = iter.firstNonNull(); // Optional.of("found")
     * }</pre>
     *
     * @return an Optional containing the first non-null element, or empty if none found
     */
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
     * Returns the last element of this iterator wrapped in a Nullable.
     * If the iterator is empty, returns an empty Nullable.
     * This operation consumes all remaining elements.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3, 4));
     * Nullable<Integer> last = iter.last(); // Nullable.of(4)
     * }</pre>
     *
     * @return a Nullable containing the last element, or empty if no elements
     */
    public Nullable<T> last() {
        if (hasNext()) {
            T next = next();

            while (hasNext()) {
                next = next();
            }

            return Nullable.of(next);
        } else {
            return Nullable.empty();
        }
    }

    /**
     * Converts the remaining elements in this iterator to an Object array.
     * This operation consumes all remaining elements in forward direction.
     *
     * @return an array containing all remaining elements
     */
    public Object[] toArray() {
        return toArray(N.EMPTY_OBJECT_ARRAY);
    }

    /**
     * Converts the remaining elements in this iterator to an array of the specified type.
     * If the provided array is large enough, it is used directly. Otherwise, a new
     * array of the same type is allocated.
     * This operation consumes all remaining elements in forward direction.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
     * String[] array = iter.toArray(new String[0]); // {"a", "b", "c"}
     * }</pre>
     *
     * @param <A> the component type of the array
     * @param a the array into which elements are stored, if big enough
     * @return an array containing all remaining elements
     */
    public <A> A[] toArray(final A[] a) {
        return toList().toArray(a);
    }

    /**
     * Converts the remaining elements in this iterator to a List.
     * This operation consumes all remaining elements in forward direction.
     * The returned list is mutable and can be modified.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ObjListIterator<Integer> iter = ObjListIterator.of(Arrays.asList(1, 2, 3));
     * List<Integer> list = iter.toList(); // [1, 2, 3]
     * }</pre>
     *
     * @return a List containing all remaining elements
     */
    public List<T> toList() {
        final List<T> list = new ArrayList<>();

        while (hasNext()) {
            list.add(next());
        }

        return list;
    }

    /**
     * Returns a Stream containing the remaining elements of this iterator.
     * This operation creates a new Stream that will consume elements from this iterator
     * in forward direction. The iterator should not be used after calling this method.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
     * long count = iter.stream()
     *     .filter(s -> s.length() > 0)
     *     .count(); // 3
     * }</pre>
     *
     * @return a Stream of the remaining elements
     */
    public Stream<T> stream() {
        return Stream.of(this);
    }

    /**
     * Performs the given action for each remaining element in forward direction.
     * This is a terminal operation that consumes all remaining elements.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * ObjListIterator<String> iter = ObjListIterator.of(Arrays.asList("a", "b", "c"));
     * iter.foreachRemaining(System.out::println);
     * // Prints: a, b, c
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown
     * @param action the action to perform for each element
     * @throws IllegalArgumentException if action is null
     * @throws E if the action throws an exception
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
     * <p>Example:</p>
     * <pre>{@code
     * List<String> list = Arrays.asList("a", "b", "c");
     * ObjListIterator<String> iter = ObjListIterator.of(list);
     * iter.next(); // Skip first element
     * iter.foreachIndexed((index, value) -> 
     *     System.out.println(index + ": " + value)
     * );
     * // Prints:
     * // 1: b
     * // 2: c
     * }</pre>
     *
     * @param <E> the type of exception that may be thrown
     * @param action the action to perform for each element and its index
     * @throws IllegalArgumentException if action is null
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void foreachIndexed(final Throwables.IntObjConsumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        int idx = 0;

        while (hasNext()) {
            action.accept(idx++, next());
        }
    }
}
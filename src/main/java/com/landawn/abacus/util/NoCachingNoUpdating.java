/*
 * Copyright (C) 2019 HaiYang Li
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

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.Stateful;
import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * Interface marking objects that should not be cached or updated.
 * This is a marker interface for one-off objects that are designed to be used once and discarded.
 * To persist or modify the object, call the {@code copy()} method (or one of the {@code toXxx()}
 * conversion methods) to create an independent, mutable copy.
 *
 * <p>This pattern is useful for objects that wrap temporary resources or provide read-only views
 * of data that should not be retained in memory.</p>
 *
 * <p><b>IMPORTANT - Reused-instance contract:</b> Implementations of this interface (and the
 * array/collection they wrap) are typically <i>reused</i> by the producer across successive
 * elements (for example, the same wrapper instance and its backing array are handed out on every
 * iteration of a stream or loop). Therefore:</p>
 * <ul>
 *   <li>Do <b>NOT</b> store or cache a {@code NoCachingNoUpdating} instance, nor the array,
 *       {@code Deque}, {@code Map.Entry}, or other backing object exposed by it (including the
 *       value returned by {@code values()} or passed to {@code apply(...)} / {@code accept(...)}).
 *       Its contents may change as soon as control returns to the producer.</li>
 *   <li>Do <b>NOT</b> modify the wrapped data; these views are intended to be read-only.</li>
 *   <li>If the data must outlive the current step, take a defensive copy via {@code copy()},
 *       {@code toArray(...)}, {@code toList()}, {@code toSet()}, or {@code toCollection(...)} —
 *       those return fresh instances that are safe to retain and modify.</li>
 * </ul>
 *
 */
@Beta
@SequentialOnly
@Stateful
public interface NoCachingNoUpdating {

    /**
     * A wrapper class for arrays that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of an array with utility methods for accessing
     * and transforming the data. The array itself should never be cached or modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] array = {"a", "b", "c"};
     * DisposableArray<String> disposable = DisposableArray.wrap(array);
     * String first = disposable.get(0);          // "a"
     * List<String> list = disposable.toList();   // creates a new list
     * }</pre>
     *
     * @param <T> the type of elements in the array
     */
    @Beta
    @SequentialOnly
    @Stateful
    @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
    class DisposableArray<T> implements NoCachingNoUpdating, Iterable<T> {

        /** The element array */
        private final T[] a;

        /**
         * Constructs a DisposableArray with the specified array.
         * The array must not be {@code null}.
         *
         * @param a the array to wrap
         * @throws IllegalArgumentException if the array is {@code null}
         */
        protected DisposableArray(final T[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableArray with a new array of the specified component type and length.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> array = DisposableArray.create(String.class, 10);
         * }</pre>
         *
         * @param <T> the type of elements in the array
         * @param componentType the class of the array elements
         * @param len the length of the array; must be non-negative
         * @return a new DisposableArray instance backed by a freshly allocated array
         * @throws IllegalArgumentException if {@code len} is negative
         */
        public static <T> DisposableArray<T> create(final Class<T> componentType, final int len) {
            if (len < 0) {
                throw new IllegalArgumentException("Length must be non-negative: " + len);
            }
            return new DisposableArray<>(N.newArray(componentType, len));
        }

        /**
         * Wraps an existing array in a DisposableArray.
         * The array is not copied; the DisposableArray provides a view of the original array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Integer[] numbers = {1, 2, 3, 4, 5};
         * DisposableArray<Integer> disposable = DisposableArray.wrap(numbers);
         * }</pre>
         *
         * @param <T> the type of elements in the array
         * @param a the array to wrap; must not be {@code null}
         * @return a new DisposableArray wrapping the given array
         * @throws IllegalArgumentException if {@code a} is {@code null}
         */
        public static <T> DisposableArray<T> wrap(final T[] a) {
            return new DisposableArray<>(a);
        }

        /**
         * Returns the element at the specified index.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> arr = DisposableArray.wrap(new String[] {"a", "b", "c"});
         * arr.get(0);   // returns "a"
         * arr.get(2);   // returns "c"
         * arr.get(-1);  // throws ArrayIndexOutOfBoundsException
         * arr.get(3);   // throws ArrayIndexOutOfBoundsException
         * }</pre>
         *
         * @param index the index of the element to return
         * @return the element at the specified index
         * @throws ArrayIndexOutOfBoundsException if the index is out of range
         */
        public T get(final int index) {
            return a[index];
        }

        /**
         * Returns the length of the wrapped array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> arr = DisposableArray.wrap(new String[] {"a", "b", "c"});
         * arr.length();                                          // returns 3
         * DisposableArray.wrap(new String[0]).length();          // returns 0
         * DisposableArray.create(String.class, 10).length();     // returns 10
         * DisposableArray.wrap(new String[] {"x"}).length();     // returns 1
         * }</pre>
         *
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Copies the elements to the specified array.
         * If the target array is too small, a new array of the same type is allocated.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> disposable = DisposableArray.wrap(new String[] {"a", "b", "c"});
         * String[] copy = disposable.toArray(new String[0]);
         * }</pre>
         *
         * <p>The returned array is safe to cache and modify.</p>
         *
         * @param <A> the runtime type of the target array
         * @param target the array into which the elements are to be stored if it is large enough;
         *               otherwise a new array of the same runtime type is allocated
         * @return an array containing the elements; the supplied {@code target} if it was large
         *         enough, otherwise a newly allocated array
         * @throws IllegalArgumentException if {@code target} is {@code null}
         */
        @SuppressWarnings("unchecked")
        public <A> A[] toArray(A[] target) {
            N.checkArgNotNull(target, "target");

            final int len = length();

            if (target.length < len) {
                target = (A[]) java.lang.reflect.Array.newInstance(target.getClass().getComponentType(), len);
            } else if (target.length > len) {
                // Mirror Collection.toArray(T[]) contract: when the supplied array is larger
                // than the data, set target[length()] to null as the end-of-data sentinel.
                target[len] = null;
            }

            N.copy(a, 0, target, 0, len);

            return target;
        }

        /**
         * Creates a new array containing the same elements as the wrapped array.
         * Use this method when the array data must be cached, retained, or modified, since
         * the wrapped array itself may be reused by the producer and must not be stored.
         * The returned array is a fresh instance independent of this DisposableArray.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> arr = DisposableArray.wrap(new String[] {"a", "b", "c"});
         * String[] copy = arr.copy();   // returns ["a", "b", "c"]
         * DisposableArray<String> empty = DisposableArray.wrap(new String[0]);
         * empty.copy();                  // returns empty String[]
         * DisposableArray<String> single = DisposableArray.wrap(new String[] {"x"});
         * single.copy();                 // returns ["x"]
         * arr.copy() != arr.copy();      // true (independent copies)
         * }</pre>
         *
         * @return a new array containing copies of the elements
         */
        public T[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the array to a List.
         * The returned list is a new instance and can be safely cached or modified.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> arr = DisposableArray.wrap(new String[] {"a", "b", "c"});
         * arr.toList();                                   // returns ["a", "b", "c"]
         * DisposableArray.wrap(new String[0]).toList();   // returns []
         * DisposableArray<String> dup = DisposableArray.wrap(new String[] {"a", "a", "b"});
         * dup.toList();                                        // returns ["a", "a", "b"] (keeps duplicates)
         * DisposableArray.wrap(new String[] {"x"}).toList();   // returns ["x"]
         * }</pre>
         *
         * @return a new List containing the array elements
         */
        public List<T> toList() {
            return N.toList(a);
        }

        /**
         * Converts the array to a Set.
         * The returned set is a new instance and can be safely cached or modified.
         * Duplicate elements in the array will appear only once in the set.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> arr = DisposableArray.wrap(new String[] {"a", "b", "c"});
         * arr.toSet();   // returns Set containing "a", "b", "c"
         * DisposableArray<String> dup = DisposableArray.wrap(new String[] {"a", "a", "b"});
         * dup.toSet();                                        // returns Set containing "a", "b" (duplicates removed)
         * DisposableArray.wrap(new String[0]).toSet();        // returns empty Set
         * DisposableArray.wrap(new String[] {"x"}).toSet();   // returns Set containing "x"
         * }</pre>
         *
         * @return a new Set containing the unique array elements
         */
        public Set<T> toSet() {
            return N.toSet(a);
        }

        /**
         * Converts the array to a Collection of the specified type.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> disposable = DisposableArray.wrap(new String[] {"a", "b", "c"});
         * List<String> list = disposable.toCollection(ArrayList::new);
         * }</pre>
         *
         * @param <C> the type of the collection to create
         * @param supplier a function that creates a new collection instance with the specified capacity
         * @return a new collection containing the array elements
         */
        public <C extends Collection<T>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());
            result.addAll(toList());
            return result;
        }

        /**
         * Performs the given action for each element of the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> disposable = DisposableArray.wrap(new String[] {"a", "b", "c"});
         * disposable.foreach(System.out::println);   // prints each element
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed for each element
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void foreach(final Throwables.Consumer<? super T, E> action) throws E {
            for (final T e : a) {
                action.accept(e);
            }
        }

        /**
         * Applies the given function to the wrapped array and returns the result.
         * This is useful for performing operations that need access to the entire array.
         *
         * <p>The live backing array is passed to {@code func}; the function must not retain a
         * reference to it or modify it. Derive an independent copy if the data must outlive the
         * call.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<Integer> disposable = DisposableArray.wrap(new Integer[] {1, 2, 3});
         * int sum = disposable.apply(arr -> Arrays.stream(arr).mapToInt(Integer::intValue).sum());
         * }</pre>
         *
         * @param <R> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the function to apply to the array
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super T[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         * Performs the given action with the wrapped array.
         * This is useful for operations that need to process the entire array without returning a value.
         *
         * <p>The live backing array is passed to {@code action}; the action must not retain a
         * reference to it or modify it.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> arr = DisposableArray.wrap(new String[] {"a", "b", "c"});
         * arr.accept(a -> System.out.println(a.length));   // prints 3
         * DisposableArray<String> empty = DisposableArray.wrap(new String[0]);
         * empty.accept(a -> System.out.println(a.length));  // prints 0
         * DisposableArray<String> single = DisposableArray.wrap(new String[] {"x"});
         * single.accept(a -> System.out.println(a[0]));    // prints "x"
         * arr.accept(a -> {});                             // invokes a no-op consumer
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to perform with the array
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super T[], E> action) throws E {
            action.accept(a);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> disposable = DisposableArray.wrap(new String[] {"a", "b", "c"});
         * String joined = disposable.join(", ");   // "a, b, c"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @return a string containing the joined elements
         */
        public String join(final String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter,
         * prefix, and suffix.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> disposable = DisposableArray.wrap(new String[] {"a", "b", "c"});
         * String joined = disposable.join(", ", "[", "]");   // "[a, b, c]"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, delimiter, prefix, suffix);
        }

        /**
         * Returns an iterator over the elements of the wrapped array.
         * The iterator reads directly from the (potentially reused) backing array; do not
         * cache the returned iterator beyond the current use.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> arr = DisposableArray.wrap(new String[] {"a", "b", "c"});
         * Iterator<String> iter = arr.iterator();
         * iter.hasNext();                                             // returns true
         * iter.next();                                                // returns "a"
         * iter.next();                                                // returns "b"
         * DisposableArray.wrap(new String[0]).iterator().hasNext();   // returns false
         * DisposableArray.wrap(new String[] {"x"}).iterator().next(); // returns "x"
         * }</pre>
         *
         * @return an iterator over the array elements
         */
        @Override
        public Iterator<T> iterator() {
            return ObjIterator.of(a);
        }

        /**
         * Returns a string representation of the wrapped array, formatted as by
         * {@link N#toString(Object[])}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableArray<String> arr = DisposableArray.wrap(new String[] {"a", "b", "c"});
         * arr.toString();                                                // returns "[a, b, c]"
         * DisposableArray.wrap(new String[0]).toString();                // returns "[]"
         * DisposableArray.wrap(new String[] {"x"}).toString();           // returns "[x]"
         * DisposableArray.wrap(new Integer[] {1, null, 3}).toString();   // returns "[1, null, 3]"
         * }</pre>
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped array directly, without copying.
         * This method is protected to prevent external access to the mutable, potentially
         * reused backing array. The returned array must not be cached or modified; use
         * {@link #copy()} when an independent array is required.
         *
         * @return the wrapped array (the live backing array, not a copy)
         */
        protected T[] values() {
            return a;
        }
    }

    /**
     * A specialized DisposableArray for Object arrays.
     * This class provides optimized handling for arrays of type Object[].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DisposableObjArray array = DisposableObjArray.create(10);
     * // or
     * Object[] objects = {1, "hello", true};
     * DisposableObjArray wrapped = DisposableObjArray.wrap(objects);
     * }</pre>
     *
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableObjArray extends DisposableArray<Object> {

        /**
         * Constructs a DisposableObjArray wrapping the specified Object array.
         *
         * @param a the Object array to wrap; must not be {@code null}
         */
        protected DisposableObjArray(final Object[] a) {
            super(a);
        }

        /**
         * Creates a new DisposableObjArray backed by a new Object array of the specified length.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableObjArray arr = DisposableObjArray.create(5);    // array has length 5, all null
         * DisposableObjArray.create(0);                             // creates an empty array
         * DisposableObjArray.create(-1);                            // throws IllegalArgumentException
         * DisposableObjArray.create(1);                             // array has length 1
         * }</pre>
         *
         * @param len the length of the array; must be non-negative
         * @return a new DisposableObjArray instance
         * @throws IllegalArgumentException if {@code len} is negative
         */
        public static DisposableObjArray create(final int len) {
            if (len < 0) {
                throw new IllegalArgumentException("Length must be non-negative: " + len);
            }
            return new DisposableObjArray(new Object[len]);
        }

        /**
         * This method is not supported for DisposableObjArray.
         * Use the {@link #create(int)} method instead.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableObjArray.create(String.class, 5);   // throws UnsupportedOperationException
         * DisposableObjArray.create(Object.class, 0);   // throws UnsupportedOperationException
         * }</pre>
         *
         * @param <T> the component type (not used)
         * @param componentType the component type (not used)
         * @param len the length (not used)
         * @return never returns
         * @throws UnsupportedOperationException always thrown
         * @deprecated Use {@link #create(int)} instead
         */
        @Deprecated
        public static <T> DisposableArray<T> create(final Class<T> componentType, final int len) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * Wraps an existing Object array in a DisposableObjArray.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableObjArray arr = DisposableObjArray.wrap(new Object[] {1, "hello", true});
         * arr.get(0);                                    // returns 1
         * DisposableObjArray.wrap(new Object[0]);        // creates a wrapper over the empty array
         * DisposableObjArray.wrap(null);                 // throws IllegalArgumentException
         * DisposableObjArray.wrap(new Object[] {"x"});   // creates a wrapper over the single-element array
         * }</pre>
         *
         * @param a the Object array to wrap; must not be {@code null}
         * @return a new DisposableObjArray wrapping the given array
         * @throws IllegalArgumentException if {@code a} is {@code null}
         */
        public static DisposableObjArray wrap(final Object[] a) {
            return new DisposableObjArray(a);
        }
    }

    /**
     * A wrapper class for boolean arrays that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of a boolean array with utility methods for accessing
     * and transforming the data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] array = {true, false, true};
     * DisposableBooleanArray disposable = DisposableBooleanArray.wrap(array);
     * boolean first = disposable.get(0);        // true
     * BooleanList list = disposable.toList();   // creates a new list
     * }</pre>
     *
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableBooleanArray implements NoCachingNoUpdating {

        /** The element array */
        private final boolean[] a;

        /**
         * Constructs a DisposableBooleanArray wrapping the specified boolean array.
         *
         * @param a the boolean array to wrap; must not be {@code null}
         */
        protected DisposableBooleanArray(final boolean[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableBooleanArray backed by a new boolean array of the specified length.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray arr = DisposableBooleanArray.create(5);    // array has length 5, all false
         * DisposableBooleanArray.create(0);                                 // creates an empty array
         * DisposableBooleanArray.create(-1);                                // throws IllegalArgumentException
         * DisposableBooleanArray.create(1);                                 // array has length 1
         * }</pre>
         *
         * @param len the length of the array; must be non-negative
         * @return a new DisposableBooleanArray instance
         * @throws IllegalArgumentException if {@code len} is negative
         */
        public static DisposableBooleanArray create(final int len) {
            if (len < 0) {
                throw new IllegalArgumentException("Length must be non-negative: " + len);
            }
            return new DisposableBooleanArray(new boolean[len]);
        }

        /**
         * Wraps an existing boolean array in a DisposableBooleanArray.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray arr = DisposableBooleanArray.wrap(new boolean[] {true, false, true});
         * arr.get(0);                                           // returns true
         * DisposableBooleanArray.wrap(new boolean[0]);          // creates a wrapper over the empty array
         * DisposableBooleanArray.wrap(null);                    // throws IllegalArgumentException
         * DisposableBooleanArray.wrap(new boolean[] {false});   // creates a wrapper over the single-element array
         * }</pre>
         *
         * @param a the boolean array to wrap; must not be {@code null}
         * @return a new DisposableBooleanArray wrapping the given array
         * @throws IllegalArgumentException if {@code a} is {@code null}
         */
        public static DisposableBooleanArray wrap(final boolean[] a) {
            return new DisposableBooleanArray(a);
        }

        /**
         * Returns the boolean value at the specified index.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray arr = DisposableBooleanArray.wrap(new boolean[] {true, false, true});
         * arr.get(0);    // returns true
         * arr.get(1);    // returns false
         * arr.get(-1);   // throws ArrayIndexOutOfBoundsException
         * arr.get(3);    // throws ArrayIndexOutOfBoundsException
         * }</pre>
         *
         * @param index the index of the element to return
         * @return the boolean value at the specified index
         * @throws ArrayIndexOutOfBoundsException if the index is out of range
         */
        public boolean get(final int index) { // NOSONAR
            return a[index];
        }

        /**
         * Returns the length of the wrapped boolean array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray arr = DisposableBooleanArray.wrap(new boolean[] {true, false, true});
         * arr.length();                                                 // returns 3
         * DisposableBooleanArray.wrap(new boolean[0]).length();         // returns 0
         * DisposableBooleanArray.create(10).length();                   // returns 10
         * DisposableBooleanArray.wrap(new boolean[] {false}).length();  // returns 1
         * }</pre>
         *
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a new boolean array containing the same elements as the wrapped array.
         * Use this method when the data must be retained or modified, since the wrapped
         * array may be reused by the producer and must not be stored. The returned array
         * is independent of this instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray arr = DisposableBooleanArray.wrap(new boolean[] {true, false, true});
         * arr.copy();                                                  // returns [true, false, true]
         * DisposableBooleanArray.wrap(new boolean[0]).copy();          // returns empty boolean[]
         * DisposableBooleanArray.wrap(new boolean[] {false}).copy();   // returns [false]
         * arr.copy() != arr.copy();                                    // true (independent copies)
         * }</pre>
         *
         * @return a new boolean array containing copies of the elements
         */
        public boolean[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the boolean array to a Boolean object array.
         * This is useful when you need to work with the wrapper type.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray arr = DisposableBooleanArray.wrap(new boolean[] {true, false, true});
         * arr.box();                                                  // returns [Boolean.TRUE, Boolean.FALSE, Boolean.TRUE]
         * DisposableBooleanArray.wrap(new boolean[0]).box();          // returns empty Boolean[]
         * DisposableBooleanArray.wrap(new boolean[] {false}).box();   // returns [Boolean.FALSE]
         * arr.box().length;                                           // returns 3
         * }</pre>
         *
         * @return a new Boolean array containing boxed values
         */
        public Boolean[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a BooleanList.
         * The returned list is a new instance and can be safely cached or modified.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray arr = DisposableBooleanArray.wrap(new boolean[] {true, false, true});
         * arr.toList();                                                  // returns BooleanList [true, false, true]
         * DisposableBooleanArray.wrap(new boolean[0]).toList();          // returns empty BooleanList
         * DisposableBooleanArray.wrap(new boolean[] {false}).toList();   // returns BooleanList [false]
         * arr.toList().size();                                           // returns 3
         * }</pre>
         *
         * @return a new BooleanList containing the array elements
         */
        public BooleanList toList() {
            return BooleanList.of(copy());
        }

        /**
         * Converts the array to a Collection of the specified type.
         * Each boolean value is boxed to Boolean.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray array = DisposableBooleanArray.wrap(new boolean[] {true, false});
         * ArrayList<Boolean> list = array.toCollection(ArrayList::new);
         * }</pre>
         *
         * @param <C> the type of the collection to create
         * @param supplier a function that creates a new collection instance with the specified capacity
         * @return a new collection containing the boxed array elements
         */
        public <C extends Collection<Boolean>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (final boolean e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         * Performs the given action for each element of the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray arr = DisposableBooleanArray.wrap(new boolean[] {true, false, true});
         * List<Boolean> collected = new ArrayList<>();
         * arr.foreach(collected::add);                                                  // collected contains [true, false, true]
         * DisposableBooleanArray.wrap(new boolean[0]).foreach(e -> {});                 // invokes nothing for the empty array
         * DisposableBooleanArray.wrap(new boolean[] {false}).foreach(collected::add);   // adds false
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed for each element
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void foreach(final Throwables.BooleanConsumer<E> action) throws E {
            for (final boolean e : a) {
                action.accept(e);
            }
        }

        /**
         * Applies the given function to the wrapped array and returns the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray arr = DisposableBooleanArray.wrap(new boolean[] {true, false, true});
         * arr.apply(a -> a.length);                                                   // returns 3
         * arr.apply(a -> { int c = 0; for (boolean b : a) if (b) c++; return c; });   // returns 2
         * DisposableBooleanArray.wrap(new boolean[0]).apply(a -> a.length);           // returns 0
         * DisposableBooleanArray.wrap(new boolean[] {false}).apply(a -> a.length);    // returns 1
         * }</pre>
         *
         * @param <R> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the function to apply to the array
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super boolean[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         * Performs the given action with the wrapped array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray arr = DisposableBooleanArray.wrap(new boolean[] {true, false, true});
         * arr.accept(a -> System.out.println(a.length));                                              // prints 3
         * DisposableBooleanArray.wrap(new boolean[0]).accept(a -> System.out.println(a.length));      // prints 0
         * DisposableBooleanArray.wrap(new boolean[] {false}).accept(a -> System.out.println(a[0]));   // prints false
         * arr.accept(a -> {});                                                                        // invokes a no-op consumer
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to perform with the array
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super boolean[], E> action) throws E {
            action.accept(a);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray arr = DisposableBooleanArray.wrap(new boolean[] {true, false, true});
         * arr.join(", ");                                                  // returns "true, false, true"
         * DisposableBooleanArray.wrap(new boolean[0]).join(", ");          // returns ""
         * DisposableBooleanArray.wrap(new boolean[] {false}).join(", ");   // returns "false"
         * arr.join("-");                                                   // returns "true-false-true"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @return a string containing the joined elements
         */
        public String join(final String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter,
         * prefix, and suffix.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray arr = DisposableBooleanArray.wrap(new boolean[] {true, false, true});
         * arr.join(", ", "[", "]");                                                  // returns "[true, false, true]"
         * DisposableBooleanArray.wrap(new boolean[0]).join(", ", "[", "]");          // returns "[]"
         * DisposableBooleanArray.wrap(new boolean[] {false}).join(", ", "[", "]");   // returns "[false]"
         * arr.join("-", "{", "}");                                                   // returns "{true-false-true}"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the wrapped boolean array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableBooleanArray arr = DisposableBooleanArray.wrap(new boolean[] {true, false, true});
         * arr.toString();                                                  // returns string representation containing true, false
         * DisposableBooleanArray.wrap(new boolean[0]).toString();          // returns string representation of empty array
         * DisposableBooleanArray.wrap(new boolean[] {false}).toString();   // returns string representation containing false
         * }</pre>
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped boolean array directly, without copying.
         * The returned array must not be cached or modified; use {@link #copy()} when an
         * independent array is required.
         *
         * @return the wrapped boolean array (the live backing array, not a copy)
         */
        protected boolean[] values() {
            return a;
        }
    }

    /**
     * A wrapper class for char arrays that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of a char array with utility methods for accessing,
     * transforming, and performing calculations on the data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] array = {'a', 'b', 'c'};
     * DisposableCharArray disposable = DisposableCharArray.wrap(array);
     * char first = disposable.get(0);   // 'a'
     * int sum = disposable.sum();       // sums the char values
     * }</pre>
     *
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableCharArray implements NoCachingNoUpdating {

        /** The element array */
        private final char[] a;

        /**
         * Constructs a DisposableCharArray wrapping the specified char array.
         *
         * @param a the char array to wrap; must not be {@code null}
         */
        protected DisposableCharArray(final char[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableCharArray backed by a new char array of the specified length.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.create(5);    // array has length 5, all '\0'
         * DisposableCharArray.create(0);                              // creates an empty array
         * DisposableCharArray.create(-1);                             // throws IllegalArgumentException
         * DisposableCharArray.create(1);                              // array has length 1
         * }</pre>
         *
         * @param len the length of the array; must be non-negative
         * @return a new DisposableCharArray instance
         * @throws IllegalArgumentException if {@code len} is negative
         */
        public static DisposableCharArray create(final int len) {
            if (len < 0) {
                throw new IllegalArgumentException("Length must be non-negative: " + len);
            }
            return new DisposableCharArray(new char[len]);
        }

        /**
         * Wraps an existing char array in a DisposableCharArray.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.get(0);                                   // returns 'a'
         * DisposableCharArray.wrap(new char[0]);        // creates a wrapper over the empty array
         * DisposableCharArray.wrap(null);               // throws IllegalArgumentException
         * DisposableCharArray.wrap(new char[] {'x'});   // creates a wrapper over the single-element array
         * }</pre>
         *
         * @param a the char array to wrap; must not be {@code null}
         * @return a new DisposableCharArray wrapping the given array
         * @throws IllegalArgumentException if {@code a} is {@code null}
         */
        public static DisposableCharArray wrap(final char[] a) {
            return new DisposableCharArray(a);
        }

        /**
         * Returns the char value at the specified index.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.get(0);    // returns 'a'
         * arr.get(2);    // returns 'c'
         * arr.get(-1);   // throws ArrayIndexOutOfBoundsException
         * arr.get(3);    // throws ArrayIndexOutOfBoundsException
         * }</pre>
         *
         * @param index the index of the element to return
         * @return the char value at the specified index
         * @throws ArrayIndexOutOfBoundsException if the index is out of range
         */
        public char get(final int index) {
            return a[index];
        }

        /**
         * Returns the length of the wrapped char array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.length();                                         // returns 3
         * DisposableCharArray.wrap(new char[0]).length();       // returns 0
         * DisposableCharArray.create(10).length();              // returns 10
         * DisposableCharArray.wrap(new char[] {'x'}).length();  // returns 1
         * }</pre>
         *
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a new char array containing the same elements as the wrapped array.
         * Use this method when the data must be retained or modified, since the wrapped
         * array may be reused by the producer and must not be stored. The returned array
         * is independent of this instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.copy();                                          // returns ['a', 'b', 'c']
         * DisposableCharArray.wrap(new char[0]).copy();        // returns empty char[]
         * DisposableCharArray.wrap(new char[] {'x'}).copy();   // returns ['x']
         * arr.copy() != arr.copy();                            // true (independent copies)
         * }</pre>
         *
         * @return a new char array containing copies of the elements
         */
        public char[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the char array to a Character object array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.box();                                          // returns [Character.valueOf('a'), Character.valueOf('b'), Character.valueOf('c')]
         * DisposableCharArray.wrap(new char[0]).box();        // returns empty Character[]
         * DisposableCharArray.wrap(new char[] {'x'}).box();   // returns [Character.valueOf('x')]
         * arr.box().length;                                   // returns 3
         * }</pre>
         *
         * @return a new Character array containing boxed values
         */
        public Character[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a CharList.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.toList();                                          // returns CharList ['a', 'b', 'c']
         * DisposableCharArray.wrap(new char[0]).toList();        // returns empty CharList
         * DisposableCharArray.wrap(new char[] {'x'}).toList();   // returns CharList ['x']
         * arr.toList().size();                                   // returns 3
         * }</pre>
         *
         * @return a new CharList containing the array elements
         */
        public CharList toList() {
            return CharList.of(copy());
        }

        /**
         * Converts the array to a Collection of the specified type.
         * Each char value is boxed to Character.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.toCollection(ArrayList::new);                                          // returns [a, b, c]
         * DisposableCharArray.wrap(new char[0]).toCollection(ArrayList::new);        // returns empty list
         * DisposableCharArray.wrap(new char[] {'x'}).toCollection(ArrayList::new);   // returns [x]
         * arr.toCollection(HashSet::new);                                            // returns Set containing a, b, c
         * }</pre>
         *
         * @param <C> the type of the collection to create
         * @param supplier a function that creates a new collection instance with the specified capacity
         * @return a new collection containing the boxed array elements
         */
        public <C extends Collection<Character>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (final char e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         * Calculates the sum of all char values in the array.
         * The char values are treated as their numeric Unicode values.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.sum();                                                   // returns 294 (97 + 98 + 99)
         * DisposableCharArray.wrap(new char[0]).sum();                 // returns 0
         * DisposableCharArray.wrap(new char[] {'A'}).sum();            // returns 65
         * DisposableCharArray.wrap(new char[] {'\0', '\0'}).sum();     // returns 0
         * }</pre>
         *
         * @return the sum of all elements
         */
        public int sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all char values in the array.
         * The char values are treated as their numeric Unicode values.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.average();                                               // returns 98.0
         * DisposableCharArray.wrap(new char[0]).average();             // returns 0.0
         * DisposableCharArray.wrap(new char[] {'a'}).average();        // returns 97.0
         * DisposableCharArray.wrap(new char[] {'A', 'B'}).average();   // returns 65.5
         * }</pre>
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum char value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'c', 'a', 'b'});
         * arr.min();                                               // returns 'a'
         * DisposableCharArray.wrap(new char[] {'z'}).min();        // returns 'z'
         * DisposableCharArray.wrap(new char[0]).min();             // throws IllegalArgumentException
         * DisposableCharArray.wrap(new char[] {'A', 'a'}).min();   // returns 'A' (65 < 97)
         * }</pre>
         *
         * @return the minimum value
         * @throws IllegalArgumentException if the array is empty
         */
        public char min() {
            return N.min(a);
        }

        /**
         * Finds the maximum char value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'c', 'b'});
         * arr.max();                                               // returns 'c'
         * DisposableCharArray.wrap(new char[] {'a'}).max();        // returns 'a'
         * DisposableCharArray.wrap(new char[0]).max();             // throws IllegalArgumentException
         * DisposableCharArray.wrap(new char[] {'A', 'a'}).max();   // returns 'a' (97 > 65)
         * }</pre>
         *
         * @return the maximum value
         * @throws IllegalArgumentException if the array is empty
         */
        public char max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * List<Character> collected = new ArrayList<>();
         * arr.foreach(collected::add);                                          // collected contains [a, b, c]
         * DisposableCharArray.wrap(new char[0]).foreach(e -> {});               // invokes nothing for the empty array
         * DisposableCharArray.wrap(new char[] {'x'}).foreach(collected::add);   // adds 'x'
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed for each element
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void foreach(final Throwables.CharConsumer<E> action) throws E {
            for (final char e : a) {
                action.accept(e);
            }
        }

        /**
         * Applies the given function to the wrapped array and returns the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.apply(a -> a.length);                                          // returns 3
         * arr.apply(a -> (int) a[0]);                                        // returns 97
         * DisposableCharArray.wrap(new char[0]).apply(a -> a.length);        // returns 0
         * DisposableCharArray.wrap(new char[] {'x'}).apply(a -> a.length);   // returns 1
         * }</pre>
         *
         * @param <R> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the function to apply to the array
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super char[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         * Performs the given action with the wrapped array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.accept(a -> System.out.println(a.length));                                      // prints 3
         * DisposableCharArray.wrap(new char[0]).accept(a -> System.out.println(a.length));    // prints 0
         * DisposableCharArray.wrap(new char[] {'x'}).accept(a -> System.out.println(a[0]));   // prints 'x'
         * arr.accept(a -> {});                                                                // invokes a no-op consumer
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to perform with the array
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super char[], E> action) throws E {
            action.accept(a);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.join(", ");                                          // returns "a, b, c"
         * DisposableCharArray.wrap(new char[0]).join(", ");        // returns ""
         * DisposableCharArray.wrap(new char[] {'x'}).join(", ");   // returns "x"
         * arr.join("-");                                           // returns "a-b-c"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @return a string containing the joined elements
         */
        public String join(final String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter,
         * prefix, and suffix.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.join(", ", "[", "]");                                          // returns "[a, b, c]"
         * DisposableCharArray.wrap(new char[0]).join(", ", "[", "]");        // returns "[]"
         * DisposableCharArray.wrap(new char[] {'x'}).join(", ", "[", "]");   // returns "[x]"
         * arr.join("-", "{", "}");                                           // returns "{a-b-c}"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the wrapped char array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableCharArray arr = DisposableCharArray.wrap(new char[] {'a', 'b', 'c'});
         * arr.toString();                                          // returns string representation of [a, b, c]
         * DisposableCharArray.wrap(new char[0]).toString();        // returns string representation of empty array
         * DisposableCharArray.wrap(new char[] {'x'}).toString();   // returns string representation of [x]
         * }</pre>
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped char array directly, without copying.
         * The returned array must not be cached or modified; use {@link #copy()} when an
         * independent array is required.
         *
         * @return the wrapped char array (the live backing array, not a copy)
         */
        protected char[] values() {
            return a;
        }
    }

    /**
     * A wrapper class for byte arrays that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of a byte array with utility methods for accessing,
     * transforming, and performing calculations on the data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] array = {1, 2, 3, 4, 5};
     * DisposableByteArray disposable = DisposableByteArray.wrap(array);
     * byte first = disposable.get(0);   // 1
     * int sum = disposable.sum();       // 15
     * }</pre>
     *
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableByteArray implements NoCachingNoUpdating {

        /** The element array */
        private final byte[] a;

        /**
         * Constructs a DisposableByteArray wrapping the specified byte array.
         *
         * @param a the byte array to wrap; must not be {@code null}
         */
        protected DisposableByteArray(final byte[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableByteArray backed by a new byte array of the specified length.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray arr = DisposableByteArray.create(5);    // array has length 5, all 0
         * DisposableByteArray.create(0);                              // creates an empty array
         * DisposableByteArray.create(-1);                             // throws IllegalArgumentException
         * DisposableByteArray.create(1);                              // array has length 1
         * }</pre>
         *
         * @param len the length of the array; must be non-negative
         * @return a new DisposableByteArray instance
         * @throws IllegalArgumentException if {@code len} is negative
         */
        public static DisposableByteArray create(final int len) {
            if (len < 0) {
                throw new IllegalArgumentException("Length must be non-negative: " + len);
            }
            return new DisposableByteArray(new byte[len]);
        }

        /**
         * Wraps an existing byte array in a DisposableByteArray.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray arr = DisposableByteArray.wrap(new byte[] {1, 2, 3});
         * arr.get(0);                                // returns 1
         * DisposableByteArray.wrap(new byte[0]);     // creates a wrapper over the empty array
         * DisposableByteArray.wrap(null);            // throws IllegalArgumentException
         * DisposableByteArray.wrap(new byte[] {99}); // creates a wrapper over the single-element array
         * }</pre>
         *
         * @param a the byte array to wrap; must not be {@code null}
         * @return a new DisposableByteArray wrapping the given array
         * @throws IllegalArgumentException if {@code a} is {@code null}
         */
        public static DisposableByteArray wrap(final byte[] a) {
            return new DisposableByteArray(a);
        }

        /**
         * Returns the byte value at the specified index.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray arr = DisposableByteArray.wrap(new byte[] {1, 2, 3});
         * arr.get(0);    // returns 1
         * arr.get(2);    // returns 3
         * arr.get(-1);   // throws ArrayIndexOutOfBoundsException
         * arr.get(3);    // throws ArrayIndexOutOfBoundsException
         * }</pre>
         *
         * @param index the index of the element to return
         * @return the byte value at the specified index
         * @throws ArrayIndexOutOfBoundsException if the index is out of range
         */
        public byte get(final int index) {
            return a[index];
        }

        /**
         * Returns the length of the wrapped byte array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray.wrap(new byte[] {1, 2, 3}).length();   // returns 3
         * DisposableByteArray.wrap(new byte[0]).length();            // returns 0
         * DisposableByteArray.create(10).length();                   // returns 10
         * DisposableByteArray.wrap(new byte[] {99}).length();        // returns 1
         * }</pre>
         *
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a new byte array containing the same elements as the wrapped array.
         * Use this method when the data must be retained or modified, since the wrapped
         * array may be reused by the producer and must not be stored. The returned array
         * is independent of this instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray arr = DisposableByteArray.wrap(new byte[] {1, 2, 3});
         * arr.copy();                                         // returns [1, 2, 3]
         * DisposableByteArray.wrap(new byte[0]).copy();       // returns empty byte[]
         * DisposableByteArray.wrap(new byte[] {99}).copy();   // returns [99]
         * arr.copy() != arr.copy();                           // true (independent copies)
         * }</pre>
         *
         * @return a new byte array containing copies of the elements
         */
        public byte[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the byte array to a Byte object array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray arr = DisposableByteArray.wrap(new byte[] {1, 2, 3});
         * arr.box();                                         // returns [Byte.valueOf((byte)1), Byte.valueOf((byte)2), Byte.valueOf((byte)3)]
         * DisposableByteArray.wrap(new byte[0]).box();       // returns empty Byte[]
         * DisposableByteArray.wrap(new byte[] {99}).box();   // returns [Byte.valueOf((byte)99)]
         * arr.box().length;                                  // returns 3
         * }</pre>
         *
         * @return a new Byte array containing boxed values
         */
        public Byte[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a ByteList.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray arr = DisposableByteArray.wrap(new byte[] {1, 2, 3});
         * arr.toList();                                         // returns ByteList [1, 2, 3]
         * DisposableByteArray.wrap(new byte[0]).toList();       // returns empty ByteList
         * DisposableByteArray.wrap(new byte[] {99}).toList();   // returns ByteList [99]
         * arr.toList().size();                                  // returns 3
         * }</pre>
         *
         * @return a new ByteList containing the array elements
         */
        public ByteList toList() {
            return ByteList.of(copy());
        }

        /**
         * Converts the array to a Collection of the specified type.
         * Each byte value is boxed to Byte.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray arr = DisposableByteArray.wrap(new byte[] {1, 2, 3});
         * arr.toCollection(ArrayList::new);                                         // returns [1, 2, 3]
         * DisposableByteArray.wrap(new byte[0]).toCollection(ArrayList::new);       // returns empty list
         * DisposableByteArray.wrap(new byte[] {99}).toCollection(ArrayList::new);   // returns [99]
         * arr.toCollection(HashSet::new);                                           // returns Set containing 1, 2, 3
         * }</pre>
         *
         * @param <C> the type of the collection to create
         * @param supplier a function that creates a new collection instance with the specified capacity
         * @return a new collection containing the boxed array elements
         */
        public <C extends Collection<Byte>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (final byte e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         * Calculates the sum of all byte values in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray.wrap(new byte[] {1, 2, 3}).sum();   // returns 6
         * DisposableByteArray.wrap(new byte[0]).sum();            // returns 0
         * DisposableByteArray.wrap(new byte[] {127}).sum();       // returns 127
         * DisposableByteArray.wrap(new byte[] {-1, 1}).sum();     // returns 0
         * }</pre>
         *
         * @return the sum of all elements
         */
        public int sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all byte values in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray.wrap(new byte[] {1, 2, 3}).average();   // returns 2.0
         * DisposableByteArray.wrap(new byte[0]).average();            // returns 0.0
         * DisposableByteArray.wrap(new byte[] {5}).average();         // returns 5.0
         * DisposableByteArray.wrap(new byte[] {1, 3}).average();      // returns 2.0
         * }</pre>
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum byte value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray.wrap(new byte[] {3, 1, 2}).min();    // returns 1
         * DisposableByteArray.wrap(new byte[] {5}).min();          // returns 5
         * DisposableByteArray.wrap(new byte[0]).min();             // throws IllegalArgumentException
         * DisposableByteArray.wrap(new byte[] {-1, 0, 1}).min();   // returns -1
         * }</pre>
         *
         * @return the minimum value
         * @throws IllegalArgumentException if the array is empty
         */
        public byte min() {
            return N.min(a);
        }

        /**
         * Finds the maximum byte value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray.wrap(new byte[] {1, 3, 2}).max();    // returns 3
         * DisposableByteArray.wrap(new byte[] {5}).max();          // returns 5
         * DisposableByteArray.wrap(new byte[0]).max();             // throws IllegalArgumentException
         * DisposableByteArray.wrap(new byte[] {-1, 0, 1}).max();   // returns 1
         * }</pre>
         *
         * @return the maximum value
         * @throws IllegalArgumentException if the array is empty
         */
        public byte max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray arr = DisposableByteArray.wrap(new byte[] {1, 2, 3});
         * List<Byte> collected = new ArrayList<>();
         * arr.foreach(collected::add);                                         // collected contains [1, 2, 3]
         * DisposableByteArray.wrap(new byte[0]).foreach(e -> {});              // invokes nothing for the empty array
         * DisposableByteArray.wrap(new byte[] {99}).foreach(collected::add);   // adds 99
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed for each element
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void foreach(final Throwables.ByteConsumer<E> action) throws E {
            for (final byte e : a) {
                action.accept(e);
            }
        }

        /**
         * Applies the given function to the wrapped array and returns the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray arr = DisposableByteArray.wrap(new byte[] {1, 2, 3});
         * arr.apply(a -> a.length);                                         // returns 3
         * arr.apply(a -> (int) a[0]);                                       // returns 1
         * DisposableByteArray.wrap(new byte[0]).apply(a -> a.length);       // returns 0
         * DisposableByteArray.wrap(new byte[] {99}).apply(a -> a.length);   // returns 1
         * }</pre>
         *
         * @param <R> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the function to apply to the array
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super byte[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         * Performs the given action with the wrapped array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray arr = DisposableByteArray.wrap(new byte[] {1, 2, 3});
         * arr.accept(a -> System.out.println(a.length));                                     // prints 3
         * DisposableByteArray.wrap(new byte[0]).accept(a -> System.out.println(a.length));   // prints 0
         * DisposableByteArray.wrap(new byte[] {99}).accept(a -> System.out.println(a[0]));   // prints 99
         * arr.accept(a -> {});                                                               // invokes a no-op consumer
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to perform with the array
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super byte[], E> action) throws E {
            action.accept(a);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray arr = DisposableByteArray.wrap(new byte[] {1, 2, 3});
         * arr.join(", ");                                         // returns "1, 2, 3"
         * DisposableByteArray.wrap(new byte[0]).join(", ");       // returns ""
         * DisposableByteArray.wrap(new byte[] {99}).join(", ");   // returns "99"
         * arr.join("-");                                          // returns "1-2-3"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @return a string containing the joined elements
         */
        public String join(final String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter,
         * prefix, and suffix.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray arr = DisposableByteArray.wrap(new byte[] {1, 2, 3});
         * arr.join(", ", "[", "]");                                         // returns "[1, 2, 3]"
         * DisposableByteArray.wrap(new byte[0]).join(", ", "[", "]");       // returns "[]"
         * DisposableByteArray.wrap(new byte[] {99}).join(", ", "[", "]");   // returns "[99]"
         * arr.join("-", "{", "}");                                          // returns "{1-2-3}"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the wrapped byte array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableByteArray arr = DisposableByteArray.wrap(new byte[] {1, 2, 3});
         * arr.toString();                                         // returns string representation of [1, 2, 3]
         * DisposableByteArray.wrap(new byte[0]).toString();       // returns string representation of empty array
         * DisposableByteArray.wrap(new byte[] {99}).toString();   // returns string representation of [99]
         * }</pre>
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped byte array directly, without copying.
         * The returned array must not be cached or modified; use {@link #copy()} when an
         * independent array is required.
         *
         * @return the wrapped byte array (the live backing array, not a copy)
         */
        protected byte[] values() {
            return a;
        }
    }

    /**
     * A wrapper class for short arrays that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of a short array with utility methods for accessing,
     * transforming, and performing calculations on the data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] array = {100, 200, 300};
     * DisposableShortArray disposable = DisposableShortArray.wrap(array);
     * short first = disposable.get(0);   // 100
     * int sum = disposable.sum();        // 600
     * }</pre>
     *
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableShortArray implements NoCachingNoUpdating {

        /** The element array */
        private final short[] a;

        /**
         * Constructs a DisposableShortArray wrapping the specified short array.
         *
         * @param a the short array to wrap; must not be {@code null}
         */
        protected DisposableShortArray(final short[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableShortArray backed by a new short array of the specified length.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray arr = DisposableShortArray.create(5);    // array has length 5, all 0
         * DisposableShortArray.create(0);                               // creates an empty array
         * DisposableShortArray.create(-1);                              // throws IllegalArgumentException
         * DisposableShortArray.create(1);                               // array has length 1
         * }</pre>
         *
         * @param len the length of the array; must be non-negative
         * @return a new DisposableShortArray instance
         * @throws IllegalArgumentException if {@code len} is negative
         */
        public static DisposableShortArray create(final int len) {
            if (len < 0) {
                throw new IllegalArgumentException("Length must be non-negative: " + len);
            }
            return new DisposableShortArray(new short[len]);
        }

        /**
         * Wraps an existing short array in a DisposableShortArray.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray arr = DisposableShortArray.wrap(new short[] {1, 2, 3});
         * arr.get(0);                                    // returns 1
         * DisposableShortArray.wrap(new short[0]);       // creates a wrapper over the empty array
         * DisposableShortArray.wrap(null);               // throws IllegalArgumentException
         * DisposableShortArray.wrap(new short[] {99});   // creates a wrapper over the single-element array
         * }</pre>
         *
         * @param a the short array to wrap; must not be {@code null}
         * @return a new DisposableShortArray wrapping the given array
         * @throws IllegalArgumentException if {@code a} is {@code null}
         */
        public static DisposableShortArray wrap(final short[] a) {
            return new DisposableShortArray(a);
        }

        /**
         * Returns the short value at the specified index.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray arr = DisposableShortArray.wrap(new short[] {1, 2, 3});
         * arr.get(0);    // returns 1
         * arr.get(2);    // returns 3
         * arr.get(-1);   // throws ArrayIndexOutOfBoundsException
         * arr.get(3);    // throws ArrayIndexOutOfBoundsException
         * }</pre>
         *
         * @param index the index of the element to return
         * @return the short value at the specified index
         * @throws ArrayIndexOutOfBoundsException if the index is out of range
         */
        public short get(final int index) {
            return a[index];
        }

        /**
         * Returns the length of the wrapped short array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray.wrap(new short[] {1, 2, 3}).length();   // returns 3
         * DisposableShortArray.wrap(new short[0]).length();            // returns 0
         * DisposableShortArray.create(10).length();                    // returns 10
         * DisposableShortArray.wrap(new short[] {99}).length();        // returns 1
         * }</pre>
         *
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a new short array containing the same elements as the wrapped array.
         * Use this method when the data must be retained or modified, since the wrapped
         * array may be reused by the producer and must not be stored. The returned array
         * is independent of this instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray arr = DisposableShortArray.wrap(new short[] {1, 2, 3});
         * arr.copy();                                           // returns [1, 2, 3]
         * DisposableShortArray.wrap(new short[0]).copy();       // returns empty short[]
         * DisposableShortArray.wrap(new short[] {99}).copy();   // returns [99]
         * arr.copy() != arr.copy();                             // true (independent copies)
         * }</pre>
         *
         * @return a new short array containing copies of the elements
         */
        public short[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the short array to a Short object array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray arr = DisposableShortArray.wrap(new short[] {1, 2, 3});
         * arr.box();                                           // returns [Short.valueOf(1), Short.valueOf(2), Short.valueOf(3)]
         * DisposableShortArray.wrap(new short[0]).box();       // returns empty Short[]
         * DisposableShortArray.wrap(new short[] {99}).box();   // returns [Short.valueOf(99)]
         * arr.box().length;                                    // returns 3
         * }</pre>
         *
         * @return a new Short array containing boxed values
         */
        public Short[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a ShortList.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray arr = DisposableShortArray.wrap(new short[] {1, 2, 3});
         * arr.toList();                                           // returns ShortList [1, 2, 3]
         * DisposableShortArray.wrap(new short[0]).toList();       // returns empty ShortList
         * DisposableShortArray.wrap(new short[] {99}).toList();   // returns ShortList [99]
         * arr.toList().size();                                    // returns 3
         * }</pre>
         *
         * @return a new ShortList containing the array elements
         */
        public ShortList toList() {
            return ShortList.of(copy());
        }

        /**
         * Converts the array to a Collection of the specified type.
         * Each short value is boxed to Short.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray arr = DisposableShortArray.wrap(new short[] {1, 2, 3});
         * arr.toCollection(ArrayList::new);                                           // returns [1, 2, 3]
         * DisposableShortArray.wrap(new short[0]).toCollection(ArrayList::new);       // returns empty list
         * DisposableShortArray.wrap(new short[] {99}).toCollection(ArrayList::new);   // returns [99]
         * arr.toCollection(HashSet::new);                                             // returns Set containing 1, 2, 3
         * }</pre>
         *
         * @param <C> the type of the collection to create
         * @param supplier a function that creates a new collection instance with the specified capacity
         * @return a new collection containing the boxed array elements
         */
        public <C extends Collection<Short>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (final short e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         * Calculates the sum of all short values in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray.wrap(new short[] {1, 2, 3}).sum();   // returns 6
         * DisposableShortArray.wrap(new short[0]).sum();            // returns 0
         * DisposableShortArray.wrap(new short[] {5}).sum();         // returns 5
         * DisposableShortArray.wrap(new short[] {-1, 1}).sum();     // returns 0
         * }</pre>
         *
         * @return the sum of all elements
         */
        public int sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all short values in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray.wrap(new short[] {1, 2, 3}).average();   // returns 2.0
         * DisposableShortArray.wrap(new short[0]).average();            // returns 0.0
         * DisposableShortArray.wrap(new short[] {5}).average();         // returns 5.0
         * DisposableShortArray.wrap(new short[] {1, 3}).average();      // returns 2.0
         * }</pre>
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum short value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray.wrap(new short[] {3, 1, 2}).min();    // returns 1
         * DisposableShortArray.wrap(new short[] {5}).min();          // returns 5
         * DisposableShortArray.wrap(new short[0]).min();             // throws IllegalArgumentException
         * DisposableShortArray.wrap(new short[] {-1, 0, 1}).min();   // returns -1
         * }</pre>
         *
         * @return the minimum value
         * @throws IllegalArgumentException if the array is empty
         */
        public short min() {
            return N.min(a);
        }

        /**
         * Finds the maximum short value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray.wrap(new short[] {1, 3, 2}).max();    // returns 3
         * DisposableShortArray.wrap(new short[] {5}).max();          // returns 5
         * DisposableShortArray.wrap(new short[0]).max();             // throws IllegalArgumentException
         * DisposableShortArray.wrap(new short[] {-1, 0, 1}).max();   // returns 1
         * }</pre>
         *
         * @return the maximum value
         * @throws IllegalArgumentException if the array is empty
         */
        public short max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray arr = DisposableShortArray.wrap(new short[] {1, 2, 3});
         * List<Short> collected = new ArrayList<>();
         * arr.foreach(collected::add);                                           // collected contains [1, 2, 3]
         * DisposableShortArray.wrap(new short[0]).foreach(e -> {});              // invokes nothing for the empty array
         * DisposableShortArray.wrap(new short[] {99}).foreach(collected::add);   // adds 99
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed for each element
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void foreach(final Throwables.ShortConsumer<E> action) throws E {
            for (final short e : a) {
                action.accept(e);
            }
        }

        /**
         * Applies the given function to the wrapped array and returns the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray arr = DisposableShortArray.wrap(new short[] {1, 2, 3});
         * arr.apply(a -> a.length);                                           // returns 3
         * arr.apply(a -> (int) a[0]);                                         // returns 1
         * DisposableShortArray.wrap(new short[0]).apply(a -> a.length);       // returns 0
         * DisposableShortArray.wrap(new short[] {99}).apply(a -> a.length);   // returns 1
         * }</pre>
         *
         * @param <R> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the function to apply to the array
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super short[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         * Performs the given action with the wrapped array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray arr = DisposableShortArray.wrap(new short[] {1, 2, 3});
         * arr.accept(a -> System.out.println(a.length));                                       // prints 3
         * DisposableShortArray.wrap(new short[0]).accept(a -> System.out.println(a.length));   // prints 0
         * DisposableShortArray.wrap(new short[] {99}).accept(a -> System.out.println(a[0]));   // prints 99
         * arr.accept(a -> {});                                                                 // invokes a no-op consumer
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to perform with the array
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super short[], E> action) throws E {
            action.accept(a);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray arr = DisposableShortArray.wrap(new short[] {1, 2, 3});
         * arr.join(", ");                                           // returns "1, 2, 3"
         * DisposableShortArray.wrap(new short[0]).join(", ");       // returns ""
         * DisposableShortArray.wrap(new short[] {99}).join(", ");   // returns "99"
         * arr.join("-");                                            // returns "1-2-3"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @return a string containing the joined elements
         */
        public String join(final String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter,
         * prefix, and suffix.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray arr = DisposableShortArray.wrap(new short[] {1, 2, 3});
         * arr.join(", ", "[", "]");                                           // returns "[1, 2, 3]"
         * DisposableShortArray.wrap(new short[0]).join(", ", "[", "]");       // returns "[]"
         * DisposableShortArray.wrap(new short[] {99}).join(", ", "[", "]");   // returns "[99]"
         * arr.join("-", "{", "}");                                            // returns "{1-2-3}"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the wrapped short array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableShortArray arr = DisposableShortArray.wrap(new short[] {1, 2, 3});
         * arr.toString();                                           // returns string representation of [1, 2, 3]
         * DisposableShortArray.wrap(new short[0]).toString();       // returns string representation of empty array
         * DisposableShortArray.wrap(new short[] {99}).toString();   // returns string representation of [99]
         * }</pre>
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped short array directly, without copying.
         * The returned array must not be cached or modified; use {@link #copy()} when an
         * independent array is required.
         *
         * @return the wrapped short array (the live backing array, not a copy)
         */
        protected short[] values() {
            return a;
        }
    }

    /**
     * A wrapper class for int arrays that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of an int array with utility methods for accessing,
     * transforming, and performing calculations on the data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] array = {10, 20, 30, 40, 50};
     * DisposableIntArray disposable = DisposableIntArray.wrap(array);
     * int first = disposable.get(0);       // 10
     * int sum = disposable.sum();          // 150
     * double avg = disposable.average();   // 30.0
     * }</pre>
     *
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableIntArray implements NoCachingNoUpdating {

        /** The element array */
        private final int[] a;

        /**
         * Constructs a DisposableIntArray wrapping the specified int array.
         *
         * @param a the int array to wrap; must not be {@code null}
         */
        protected DisposableIntArray(final int[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableIntArray backed by a new int array of the specified length.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray arr = DisposableIntArray.create(5);    // array has length 5, all 0
         * DisposableIntArray.create(0);                             // creates an empty array
         * DisposableIntArray.create(-1);                            // throws IllegalArgumentException
         * DisposableIntArray.create(1);                             // array has length 1
         * }</pre>
         *
         * @param len the length of the array; must be non-negative
         * @return a new DisposableIntArray instance
         * @throws IllegalArgumentException if {@code len} is negative
         */
        public static DisposableIntArray create(final int len) {
            if (len < 0) {
                throw new IllegalArgumentException("Length must be non-negative: " + len);
            }
            return new DisposableIntArray(new int[len]);
        }

        /**
         * Wraps an existing int array in a DisposableIntArray.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray arr = DisposableIntArray.wrap(new int[] {1, 2, 3});
         * arr.get(0);                                // returns 1
         * DisposableIntArray.wrap(new int[0]);       // creates a wrapper over the empty array
         * DisposableIntArray.wrap(null);             // throws IllegalArgumentException
         * DisposableIntArray.wrap(new int[] {99});   // creates a wrapper over the single-element array
         * }</pre>
         *
         * @param a the int array to wrap; must not be {@code null}
         * @return a new DisposableIntArray wrapping the given array
         * @throws IllegalArgumentException if {@code a} is {@code null}
         */
        public static DisposableIntArray wrap(final int[] a) {
            return new DisposableIntArray(a);
        }

        /**
         * Returns the int value at the specified index.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray arr = DisposableIntArray.wrap(new int[] {1, 2, 3});
         * arr.get(0);    // returns 1
         * arr.get(2);    // returns 3
         * arr.get(-1);   // throws ArrayIndexOutOfBoundsException
         * arr.get(3);    // throws ArrayIndexOutOfBoundsException
         * }</pre>
         *
         * @param index the index of the element to return
         * @return the int value at the specified index
         * @throws ArrayIndexOutOfBoundsException if the index is out of range
         */
        public int get(final int index) {
            return a[index];
        }

        /**
         * Returns the length of the wrapped int array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray.wrap(new int[] {1, 2, 3}).length();   // returns 3
         * DisposableIntArray.wrap(new int[0]).length();            // returns 0
         * DisposableIntArray.create(10).length();                  // returns 10
         * DisposableIntArray.wrap(new int[] {99}).length();        // returns 1
         * }</pre>
         *
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a new int array containing the same elements as the wrapped array.
         * Use this method when the data must be retained or modified, since the wrapped
         * array may be reused by the producer and must not be stored. The returned array
         * is independent of this instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray arr = DisposableIntArray.wrap(new int[] {1, 2, 3});
         * arr.copy();                                       // returns [1, 2, 3]
         * DisposableIntArray.wrap(new int[0]).copy();       // returns empty int[]
         * DisposableIntArray.wrap(new int[] {99}).copy();   // returns [99]
         * arr.copy() != arr.copy();                         // true (independent copies)
         * }</pre>
         *
         * @return a new int array containing copies of the elements
         */
        public int[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the int array to an Integer object array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray arr = DisposableIntArray.wrap(new int[] {1, 2, 3});
         * arr.box();                                        // returns [Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)]
         * DisposableIntArray.wrap(new int[0]).box();        // returns empty Integer[]
         * DisposableIntArray.wrap(new int[] {99}).box();    // returns [Integer.valueOf(99)]
         * arr.box().length;                                 // returns 3
         * }</pre>
         *
         * @return a new Integer array containing boxed values
         */
        public Integer[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a IntList.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray arr = DisposableIntArray.wrap(new int[] {1, 2, 3});
         * arr.toList();                                       // returns IntList [1, 2, 3]
         * DisposableIntArray.wrap(new int[0]).toList();       // returns empty IntList
         * DisposableIntArray.wrap(new int[] {99}).toList();   // returns IntList [99]
         * arr.toList().size();                                // returns 3
         * }</pre>
         *
         * @return a new IntList containing the array elements
         */
        public IntList toList() {
            return IntList.of(copy());
        }

        /**
         * Converts the array to a Collection of the specified type.
         * Each int value is boxed to Integer.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray arr = DisposableIntArray.wrap(new int[] {1, 2, 3});
         * arr.toCollection(ArrayList::new);                                       // returns [1, 2, 3]
         * DisposableIntArray.wrap(new int[0]).toCollection(ArrayList::new);       // returns empty list
         * DisposableIntArray.wrap(new int[] {99}).toCollection(ArrayList::new);   // returns [99]
         * arr.toCollection(HashSet::new);                                         // returns Set containing 1, 2, 3
         * }</pre>
         *
         * @param <C> the type of the collection to create
         * @param supplier a function that creates a new collection instance with the specified capacity
         * @return a new collection containing the boxed array elements
         */
        public <C extends Collection<Integer>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (final int e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         * Calculates the sum of all int values in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray.wrap(new int[] {1, 2, 3}).sum();   // returns 6
         * DisposableIntArray.wrap(new int[0]).sum();            // returns 0
         * DisposableIntArray.wrap(new int[] {5}).sum();         // returns 5
         * DisposableIntArray.wrap(new int[] {-1, 1}).sum();     // returns 0
         * }</pre>
         *
         * @return the sum of all elements
         */
        public int sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all int values in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray.wrap(new int[] {1, 2, 3}).average();   // returns 2.0
         * DisposableIntArray.wrap(new int[0]).average();            // returns 0.0
         * DisposableIntArray.wrap(new int[] {5}).average();         // returns 5.0
         * DisposableIntArray.wrap(new int[] {1, 3}).average();      // returns 2.0
         * }</pre>
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum int value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray.wrap(new int[] {3, 1, 2}).min();    // returns 1
         * DisposableIntArray.wrap(new int[] {5}).min();          // returns 5
         * DisposableIntArray.wrap(new int[0]).min();             // throws IllegalArgumentException
         * DisposableIntArray.wrap(new int[] {-1, 0, 1}).min();   // returns -1
         * }</pre>
         *
         * @return the minimum value
         * @throws IllegalArgumentException if the array is empty
         */
        public int min() {
            return N.min(a);
        }

        /**
         * Finds the maximum int value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray.wrap(new int[] {1, 3, 2}).max();    // returns 3
         * DisposableIntArray.wrap(new int[] {5}).max();          // returns 5
         * DisposableIntArray.wrap(new int[0]).max();             // throws IllegalArgumentException
         * DisposableIntArray.wrap(new int[] {-1, 0, 1}).max();   // returns 1
         * }</pre>
         *
         * @return the maximum value
         * @throws IllegalArgumentException if the array is empty
         */
        public int max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray arr = DisposableIntArray.wrap(new int[] {1, 2, 3});
         * List<Integer> collected = new ArrayList<>();
         * arr.foreach(collected::add);                                       // collected contains [1, 2, 3]
         * DisposableIntArray.wrap(new int[0]).foreach(e -> {});              // invokes nothing for the empty array
         * DisposableIntArray.wrap(new int[] {99}).foreach(collected::add);   // adds 99
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed for each element
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void foreach(final Throwables.IntConsumer<E> action) throws E {
            for (final int e : a) {
                action.accept(e);
            }
        }

        /**
         * Applies the given function to the wrapped array and returns the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray arr = DisposableIntArray.wrap(new int[] {1, 2, 3});
         * arr.apply(a -> a.length);                                       // returns 3
         * arr.apply(a -> (int) a[0]);                                     // returns 1
         * DisposableIntArray.wrap(new int[0]).apply(a -> a.length);       // returns 0
         * DisposableIntArray.wrap(new int[] {99}).apply(a -> a.length);   // returns 1
         * }</pre>
         *
         * @param <R> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the function to apply to the array
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super int[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         * Performs the given action with the wrapped array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray arr = DisposableIntArray.wrap(new int[] {1, 2, 3});
         * arr.accept(a -> System.out.println(a.length));                                   // prints 3
         * DisposableIntArray.wrap(new int[0]).accept(a -> System.out.println(a.length));   // prints 0
         * DisposableIntArray.wrap(new int[] {99}).accept(a -> System.out.println(a[0]));   // prints 99
         * arr.accept(a -> {});                                                             // invokes a no-op consumer
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to perform with the array
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super int[], E> action) throws E {
            action.accept(a);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray arr = DisposableIntArray.wrap(new int[] {1, 2, 3});
         * arr.join(", ");                                       // returns "1, 2, 3"
         * DisposableIntArray.wrap(new int[0]).join(", ");       // returns ""
         * DisposableIntArray.wrap(new int[] {99}).join(", ");   // returns "99"
         * arr.join("-");                                        // returns "1-2-3"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @return a string containing the joined elements
         */
        public String join(final String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter,
         * prefix, and suffix.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray arr = DisposableIntArray.wrap(new int[] {1, 2, 3});
         * arr.join(", ", "[", "]");                                       // returns "[1, 2, 3]"
         * DisposableIntArray.wrap(new int[0]).join(", ", "[", "]");       // returns "[]"
         * DisposableIntArray.wrap(new int[] {99}).join(", ", "[", "]");   // returns "[99]"
         * arr.join("-", "{", "}");                                        // returns "{1-2-3}"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the wrapped int array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableIntArray arr = DisposableIntArray.wrap(new int[] {1, 2, 3});
         * arr.toString();                                       // returns string representation of [1, 2, 3]
         * DisposableIntArray.wrap(new int[0]).toString();       // returns string representation of empty array
         * DisposableIntArray.wrap(new int[] {99}).toString();   // returns string representation of [99]
         * }</pre>
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped int array directly, without copying.
         * The returned array must not be cached or modified; use {@link #copy()} when an
         * independent array is required.
         *
         * @return the wrapped int array (the live backing array, not a copy)
         */
        protected int[] values() {
            return a;
        }
    }

    /**
     * A wrapper class for long arrays that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of a long array with utility methods for accessing,
     * transforming, and performing calculations on the data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] array = {1000L, 2000L, 3000L};
     * DisposableLongArray disposable = DisposableLongArray.wrap(array);
     * long first = disposable.get(0);   // 1000L
     * long sum = disposable.sum();      // 6000L
     * }</pre>
     *
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableLongArray implements NoCachingNoUpdating {

        /** The element array */
        private final long[] a;

        /**
         * Constructs a DisposableLongArray wrapping the specified long array.
         *
         * @param a the long array to wrap; must not be {@code null}
         */
        protected DisposableLongArray(final long[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableLongArray backed by a new long array of the specified length.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray arr = DisposableLongArray.create(5);    // array has length 5, all 0
         * DisposableLongArray.create(0);                              // creates an empty array
         * DisposableLongArray.create(-1);                             // throws IllegalArgumentException
         * DisposableLongArray.create(1);                              // array has length 1
         * }</pre>
         *
         * @param len the length of the array; must be non-negative
         * @return a new DisposableLongArray instance
         * @throws IllegalArgumentException if {@code len} is negative
         */
        public static DisposableLongArray create(final int len) {
            if (len < 0) {
                throw new IllegalArgumentException("Length must be non-negative: " + len);
            }
            return new DisposableLongArray(new long[len]);
        }

        /**
         * Wraps an existing long array in a DisposableLongArray.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray arr = DisposableLongArray.wrap(new long[] {1, 2, 3});
         * arr.get(0);                                  // returns 1
         * DisposableLongArray.wrap(new long[0]);       // creates a wrapper over the empty array
         * DisposableLongArray.wrap(null);              // throws IllegalArgumentException
         * DisposableLongArray.wrap(new long[] {99});   // creates a wrapper over the single-element array
         * }</pre>
         *
         * @param a the long array to wrap; must not be {@code null}
         * @return a new DisposableLongArray wrapping the given array
         * @throws IllegalArgumentException if {@code a} is {@code null}
         */
        public static DisposableLongArray wrap(final long[] a) {
            return new DisposableLongArray(a);
        }

        /**
         * Returns the long value at the specified index.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray arr = DisposableLongArray.wrap(new long[] {1, 2, 3});
         * arr.get(0);    // returns 1
         * arr.get(2);    // returns 3
         * arr.get(-1);   // throws ArrayIndexOutOfBoundsException
         * arr.get(3);    // throws ArrayIndexOutOfBoundsException
         * }</pre>
         *
         * @param index the index of the element to return
         * @return the long value at the specified index
         * @throws ArrayIndexOutOfBoundsException if the index is out of range
         */
        public long get(final int index) {
            return a[index];
        }

        /**
         * Returns the length of the wrapped long array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray.wrap(new long[] {1, 2, 3}).length();   // returns 3
         * DisposableLongArray.wrap(new long[0]).length();            // returns 0
         * DisposableLongArray.create(10).length();                   // returns 10
         * DisposableLongArray.wrap(new long[] {99}).length();        // returns 1
         * }</pre>
         *
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a new long array containing the same elements as the wrapped array.
         * Use this method when the data must be retained or modified, since the wrapped
         * array may be reused by the producer and must not be stored. The returned array
         * is independent of this instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray arr = DisposableLongArray.wrap(new long[] {1, 2, 3});
         * arr.copy();                                         // returns [1, 2, 3]
         * DisposableLongArray.wrap(new long[0]).copy();       // returns empty long[]
         * DisposableLongArray.wrap(new long[] {99}).copy();   // returns [99]
         * arr.copy() != arr.copy();                           // true (independent copies)
         * }</pre>
         *
         * @return a new long array containing copies of the elements
         */
        public long[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the long array to a Long object array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray arr = DisposableLongArray.wrap(new long[] {1, 2, 3});
         * arr.box();                                         // returns [Long.valueOf(1), Long.valueOf(2), Long.valueOf(3)]
         * DisposableLongArray.wrap(new long[0]).box();       // returns empty Long[]
         * DisposableLongArray.wrap(new long[] {99}).box();   // returns [Long.valueOf(99)]
         * arr.box().length;                                  // returns 3
         * }</pre>
         *
         * @return a new Long array containing boxed values
         */
        public Long[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a LongList.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray arr = DisposableLongArray.wrap(new long[] {1, 2, 3});
         * arr.toList();                                         // returns LongList [1, 2, 3]
         * DisposableLongArray.wrap(new long[0]).toList();       // returns empty LongList
         * DisposableLongArray.wrap(new long[] {99}).toList();   // returns LongList [99]
         * arr.toList().size();                                  // returns 3
         * }</pre>
         *
         * @return a new LongList containing the array elements
         */
        public LongList toList() {
            return LongList.of(copy());
        }

        /**
         * Converts the array to a Collection of the specified type.
         * Each long value is boxed to Long.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray arr = DisposableLongArray.wrap(new long[] {1, 2, 3});
         * arr.toCollection(ArrayList::new);                                         // returns [1, 2, 3]
         * DisposableLongArray.wrap(new long[0]).toCollection(ArrayList::new);       // returns empty list
         * DisposableLongArray.wrap(new long[] {99}).toCollection(ArrayList::new);   // returns [99]
         * arr.toCollection(HashSet::new);                                           // returns Set containing 1, 2, 3
         * }</pre>
         *
         * @param <C> the type of the collection to create
         * @param supplier a function that creates a new collection instance with the specified capacity
         * @return a new collection containing the boxed array elements
         */
        public <C extends Collection<Long>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (final long e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         * Calculates the sum of all long values in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray.wrap(new long[] {1, 2, 3}).sum();   // returns 6L
         * DisposableLongArray.wrap(new long[0]).sum();            // returns 0
         * DisposableLongArray.wrap(new long[] {5}).sum();         // returns 5
         * DisposableLongArray.wrap(new long[] {-1, 1}).sum();     // returns 0
         * }</pre>
         *
         * @return the sum of all elements
         */
        public long sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all long values in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray.wrap(new long[] {1, 2, 3}).average();   // returns 2.0
         * DisposableLongArray.wrap(new long[0]).average();            // returns 0.0
         * DisposableLongArray.wrap(new long[] {5}).average();         // returns 5.0
         * DisposableLongArray.wrap(new long[] {1, 3}).average();      // returns 2.0
         * }</pre>
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum long value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray.wrap(new long[] {3, 1, 2}).min();    // returns 1
         * DisposableLongArray.wrap(new long[] {5}).min();          // returns 5
         * DisposableLongArray.wrap(new long[0]).min();             // throws IllegalArgumentException
         * DisposableLongArray.wrap(new long[] {-1, 0, 1}).min();   // returns -1
         * }</pre>
         *
         * @return the minimum value
         * @throws IllegalArgumentException if the array is empty
         */
        public long min() {
            return N.min(a);
        }

        /**
         * Finds the maximum long value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray.wrap(new long[] {1, 3, 2}).max();    // returns 3
         * DisposableLongArray.wrap(new long[] {5}).max();          // returns 5
         * DisposableLongArray.wrap(new long[0]).max();             // throws IllegalArgumentException
         * DisposableLongArray.wrap(new long[] {-1, 0, 1}).max();   // returns 1
         * }</pre>
         *
         * @return the maximum value
         * @throws IllegalArgumentException if the array is empty
         */
        public long max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray arr = DisposableLongArray.wrap(new long[] {1, 2, 3});
         * List<Long> collected = new ArrayList<>();
         * arr.foreach(collected::add);                                         // collected contains [1, 2, 3]
         * DisposableLongArray.wrap(new long[0]).foreach(e -> {});              // invokes nothing for the empty array
         * DisposableLongArray.wrap(new long[] {99}).foreach(collected::add);   // adds 99
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed for each element
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void foreach(final Throwables.LongConsumer<E> action) throws E {
            for (final long e : a) {
                action.accept(e);
            }
        }

        /**
         * Applies the given function to the wrapped array and returns the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray arr = DisposableLongArray.wrap(new long[] {1, 2, 3});
         * arr.apply(a -> a.length);                                         // returns 3
         * arr.apply(a -> (int) a[0]);                                       // returns 1
         * DisposableLongArray.wrap(new long[0]).apply(a -> a.length);       // returns 0
         * DisposableLongArray.wrap(new long[] {99}).apply(a -> a.length);   // returns 1
         * }</pre>
         *
         * @param <R> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the function to apply to the array
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super long[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         * Performs the given action with the wrapped array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray arr = DisposableLongArray.wrap(new long[] {1, 2, 3});
         * arr.accept(a -> System.out.println(a.length));                                     // prints 3
         * DisposableLongArray.wrap(new long[0]).accept(a -> System.out.println(a.length));   // prints 0
         * DisposableLongArray.wrap(new long[] {99}).accept(a -> System.out.println(a[0]));   // prints 99
         * arr.accept(a -> {});                                                               // invokes a no-op consumer
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to perform with the array
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super long[], E> action) throws E {
            action.accept(a);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray arr = DisposableLongArray.wrap(new long[] {1, 2, 3});
         * arr.join(", ");                                         // returns "1, 2, 3"
         * DisposableLongArray.wrap(new long[0]).join(", ");       // returns ""
         * DisposableLongArray.wrap(new long[] {99}).join(", ");   // returns "99"
         * arr.join("-");                                          // returns "1-2-3"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @return a string containing the joined elements
         */
        public String join(final String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter,
         * prefix, and suffix.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray arr = DisposableLongArray.wrap(new long[] {1, 2, 3});
         * arr.join(", ", "[", "]");                                         // returns "[1, 2, 3]"
         * DisposableLongArray.wrap(new long[0]).join(", ", "[", "]");       // returns "[]"
         * DisposableLongArray.wrap(new long[] {99}).join(", ", "[", "]");   // returns "[99]"
         * arr.join("-", "{", "}");                                          // returns "{1-2-3}"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the wrapped long array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableLongArray arr = DisposableLongArray.wrap(new long[] {1, 2, 3});
         * arr.toString();                                         // returns string representation of [1, 2, 3]
         * DisposableLongArray.wrap(new long[0]).toString();       // returns string representation of empty array
         * DisposableLongArray.wrap(new long[] {99}).toString();   // returns string representation of [99]
         * }</pre>
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped long array directly, without copying.
         * The returned array must not be cached or modified; use {@link #copy()} when an
         * independent array is required.
         *
         * @return the wrapped long array (the live backing array, not a copy)
         */
        protected long[] values() {
            return a;
        }
    }

    /**
     * A wrapper class for float arrays that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of a float array with utility methods for accessing,
     * transforming, and performing calculations on the data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] array = {1.5f, 2.5f, 3.5f};
     * DisposableFloatArray disposable = DisposableFloatArray.wrap(array);
     * float first = disposable.get(0);   // 1.5f
     * float sum = disposable.sum();      // 7.5f
     * }</pre>
     *
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableFloatArray implements NoCachingNoUpdating {

        /** The element array */
        private final float[] a;

        /**
         * Constructs a DisposableFloatArray wrapping the specified float array.
         *
         * @param a the float array to wrap; must not be {@code null}
         */
        protected DisposableFloatArray(final float[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableFloatArray backed by a new float array of the specified length.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray arr = DisposableFloatArray.create(5);    // array has length 5, all 0
         * DisposableFloatArray.create(0);                               // creates an empty array
         * DisposableFloatArray.create(-1);                              // throws IllegalArgumentException
         * DisposableFloatArray.create(1);                               // array has length 1
         * }</pre>
         *
         * @param len the length of the array; must be non-negative
         * @return a new DisposableFloatArray instance
         * @throws IllegalArgumentException if {@code len} is negative
         */
        public static DisposableFloatArray create(final int len) {
            if (len < 0) {
                throw new IllegalArgumentException("Length must be non-negative: " + len);
            }
            return new DisposableFloatArray(new float[len]);
        }

        /**
         * Wraps an existing float array in a DisposableFloatArray.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray arr = DisposableFloatArray.wrap(new float[] {1, 2, 3});
         * arr.get(0);                                    // returns 1
         * DisposableFloatArray.wrap(new float[0]);       // creates a wrapper over the empty array
         * DisposableFloatArray.wrap(null);               // throws IllegalArgumentException
         * DisposableFloatArray.wrap(new float[] {99});   // creates a wrapper over the single-element array
         * }</pre>
         *
         * @param a the float array to wrap; must not be {@code null}
         * @return a new DisposableFloatArray wrapping the given array
         * @throws IllegalArgumentException if {@code a} is {@code null}
         */
        public static DisposableFloatArray wrap(final float[] a) {
            return new DisposableFloatArray(a);
        }

        /**
         * Returns the float value at the specified index.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray arr = DisposableFloatArray.wrap(new float[] {1, 2, 3});
         * arr.get(0);    // returns 1
         * arr.get(2);    // returns 3
         * arr.get(-1);   // throws ArrayIndexOutOfBoundsException
         * arr.get(3);    // throws ArrayIndexOutOfBoundsException
         * }</pre>
         *
         * @param index the index of the element to return
         * @return the float value at the specified index
         * @throws ArrayIndexOutOfBoundsException if the index is out of range
         */
        public float get(final int index) {
            return a[index];
        }

        /**
         * Returns the length of the wrapped float array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray.wrap(new float[] {1, 2, 3}).length();   // returns 3
         * DisposableFloatArray.wrap(new float[0]).length();            // returns 0
         * DisposableFloatArray.create(10).length();                    // returns 10
         * DisposableFloatArray.wrap(new float[] {99}).length();        // returns 1
         * }</pre>
         *
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a new float array containing the same elements as the wrapped array.
         * Use this method when the data must be retained or modified, since the wrapped
         * array may be reused by the producer and must not be stored. The returned array
         * is independent of this instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray arr = DisposableFloatArray.wrap(new float[] {1, 2, 3});
         * arr.copy();                                           // returns [1, 2, 3]
         * DisposableFloatArray.wrap(new float[0]).copy();       // returns empty float[]
         * DisposableFloatArray.wrap(new float[] {99}).copy();   // returns [99]
         * arr.copy() != arr.copy();                             // true (independent copies)
         * }</pre>
         *
         * @return a new float array containing copies of the elements
         */
        public float[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the float array to a Float object array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray arr = DisposableFloatArray.wrap(new float[] {1, 2, 3});
         * arr.box();                                           // returns [Float.valueOf(1), Float.valueOf(2), Float.valueOf(3)]
         * DisposableFloatArray.wrap(new float[0]).box();       // returns empty Float[]
         * DisposableFloatArray.wrap(new float[] {99}).box();   // returns [Float.valueOf(99)]
         * arr.box().length;                                    // returns 3
         * }</pre>
         *
         * @return a new Float array containing boxed values
         */
        public Float[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a FloatList.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray arr = DisposableFloatArray.wrap(new float[] {1, 2, 3});
         * arr.toList();                                           // returns FloatList [1, 2, 3]
         * DisposableFloatArray.wrap(new float[0]).toList();       // returns empty FloatList
         * DisposableFloatArray.wrap(new float[] {99}).toList();   // returns FloatList [99]
         * arr.toList().size();                                    // returns 3
         * }</pre>
         *
         * @return a new FloatList containing the array elements
         */
        public FloatList toList() {
            return FloatList.of(copy());
        }

        /**
         * Converts the array to a Collection of the specified type.
         * Each float value is boxed to Float.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray arr = DisposableFloatArray.wrap(new float[] {1, 2, 3});
         * arr.toCollection(ArrayList::new);                                           // returns [1, 2, 3]
         * DisposableFloatArray.wrap(new float[0]).toCollection(ArrayList::new);       // returns empty list
         * DisposableFloatArray.wrap(new float[] {99}).toCollection(ArrayList::new);   // returns [99]
         * arr.toCollection(HashSet::new);                                             // returns Set containing 1, 2, 3
         * }</pre>
         *
         * @param <C> the type of the collection to create
         * @param supplier a function that creates a new collection instance with the specified capacity
         * @return a new collection containing the boxed array elements
         */
        public <C extends Collection<Float>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (final float e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         * Calculates the sum of all float values in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray.wrap(new float[] {1, 2, 3}).sum();   // returns 6.0f
         * DisposableFloatArray.wrap(new float[0]).sum();            // returns 0
         * DisposableFloatArray.wrap(new float[] {5}).sum();         // returns 5
         * DisposableFloatArray.wrap(new float[] {-1, 1}).sum();     // returns 0
         * }</pre>
         *
         * @return the sum of all elements
         */
        public float sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all float values in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray.wrap(new float[] {1, 2, 3}).average();   // returns 2.0
         * DisposableFloatArray.wrap(new float[0]).average();            // returns 0.0
         * DisposableFloatArray.wrap(new float[] {5}).average();         // returns 5.0
         * DisposableFloatArray.wrap(new float[] {1, 3}).average();      // returns 2.0
         * }</pre>
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum float value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray.wrap(new float[] {3, 1, 2}).min();    // returns 1
         * DisposableFloatArray.wrap(new float[] {5}).min();          // returns 5
         * DisposableFloatArray.wrap(new float[0]).min();             // throws IllegalArgumentException
         * DisposableFloatArray.wrap(new float[] {-1, 0, 1}).min();   // returns -1
         * }</pre>
         *
         * @return the minimum value
         * @throws IllegalArgumentException if the array is empty
         */
        public float min() {
            return N.min(a);
        }

        /**
         * Finds the maximum float value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray.wrap(new float[] {1, 3, 2}).max();    // returns 3
         * DisposableFloatArray.wrap(new float[] {5}).max();          // returns 5
         * DisposableFloatArray.wrap(new float[0]).max();             // throws IllegalArgumentException
         * DisposableFloatArray.wrap(new float[] {-1, 0, 1}).max();   // returns 1
         * }</pre>
         *
         * @return the maximum value
         * @throws IllegalArgumentException if the array is empty
         */
        public float max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray arr = DisposableFloatArray.wrap(new float[] {1, 2, 3});
         * List<Float> collected = new ArrayList<>();
         * arr.foreach(collected::add);                                           // collected contains [1, 2, 3]
         * DisposableFloatArray.wrap(new float[0]).foreach(e -> {});              // invokes nothing for the empty array
         * DisposableFloatArray.wrap(new float[] {99}).foreach(collected::add);   // adds 99
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed for each element
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void foreach(final Throwables.FloatConsumer<E> action) throws E {
            for (final float e : a) {
                action.accept(e);
            }
        }

        /**
         * Applies the given function to the wrapped array and returns the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray arr = DisposableFloatArray.wrap(new float[] {1, 2, 3});
         * arr.apply(a -> a.length);                                           // returns 3
         * arr.apply(a -> (int) a[0]);                                         // returns 1
         * DisposableFloatArray.wrap(new float[0]).apply(a -> a.length);       // returns 0
         * DisposableFloatArray.wrap(new float[] {99}).apply(a -> a.length);   // returns 1
         * }</pre>
         *
         * @param <R> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the function to apply to the array
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super float[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         * Performs the given action with the wrapped array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray arr = DisposableFloatArray.wrap(new float[] {1, 2, 3});
         * arr.accept(a -> System.out.println(a.length));                                       // prints 3
         * DisposableFloatArray.wrap(new float[0]).accept(a -> System.out.println(a.length));   // prints 0
         * DisposableFloatArray.wrap(new float[] {99}).accept(a -> System.out.println(a[0]));   // prints 99
         * arr.accept(a -> {});                                                                 // invokes a no-op consumer
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to perform with the array
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super float[], E> action) throws E {
            action.accept(a);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray arr = DisposableFloatArray.wrap(new float[] {1, 2, 3});
         * arr.join(", ");                                           // returns "1, 2, 3"
         * DisposableFloatArray.wrap(new float[0]).join(", ");       // returns ""
         * DisposableFloatArray.wrap(new float[] {99}).join(", ");   // returns "99"
         * arr.join("-");                                            // returns "1-2-3"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @return a string containing the joined elements
         */
        public String join(final String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter,
         * prefix, and suffix.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray arr = DisposableFloatArray.wrap(new float[] {1, 2, 3});
         * arr.join(", ", "[", "]");                                           // returns "[1, 2, 3]"
         * DisposableFloatArray.wrap(new float[0]).join(", ", "[", "]");       // returns "[]"
         * DisposableFloatArray.wrap(new float[] {99}).join(", ", "[", "]");   // returns "[99]"
         * arr.join("-", "{", "}");                                            // returns "{1-2-3}"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the wrapped float array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableFloatArray arr = DisposableFloatArray.wrap(new float[] {1, 2, 3});
         * arr.toString();                                           // returns string representation of [1, 2, 3]
         * DisposableFloatArray.wrap(new float[0]).toString();       // returns string representation of empty array
         * DisposableFloatArray.wrap(new float[] {99}).toString();   // returns string representation of [99]
         * }</pre>
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped float array directly, without copying.
         * The returned array must not be cached or modified; use {@link #copy()} when an
         * independent array is required.
         *
         * @return the wrapped float array (the live backing array, not a copy)
         */
        protected float[] values() {
            return a;
        }
    }

    /**
     * A wrapper class for double arrays that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of a double array with utility methods for accessing,
     * transforming, and performing calculations on the data.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] array = {1.1, 2.2, 3.3, 4.4};
     * DisposableDoubleArray disposable = DisposableDoubleArray.wrap(array);
     * double first = disposable.get(0);    // 1.1
     * double sum = disposable.sum();       // 11.0
     * double avg = disposable.average();   // 2.75
     * }</pre>
     *
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableDoubleArray implements NoCachingNoUpdating {

        /** The element array */
        private final double[] a;

        /**
         * Constructs a DisposableDoubleArray wrapping the specified double array.
         *
         * @param a the double array to wrap; must not be {@code null}
         */
        protected DisposableDoubleArray(final double[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableDoubleArray backed by a new double array of the specified length.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray arr = DisposableDoubleArray.create(5);    // array has length 5, all 0
         * DisposableDoubleArray.create(0);                                // creates an empty array
         * DisposableDoubleArray.create(-1);                               // throws IllegalArgumentException
         * DisposableDoubleArray.create(1);                                // array has length 1
         * }</pre>
         *
         * @param len the length of the array; must be non-negative
         * @return a new DisposableDoubleArray instance
         * @throws IllegalArgumentException if {@code len} is negative
         */
        public static DisposableDoubleArray create(final int len) {
            if (len < 0) {
                throw new IllegalArgumentException("Length must be non-negative: " + len);
            }
            return new DisposableDoubleArray(new double[len]);
        }

        /**
         * Wraps an existing double array in a DisposableDoubleArray.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray arr = DisposableDoubleArray.wrap(new double[] {1, 2, 3});
         * arr.get(0);                                      // returns 1
         * DisposableDoubleArray.wrap(new double[0]);       // creates a wrapper over the empty array
         * DisposableDoubleArray.wrap(null);                // throws IllegalArgumentException
         * DisposableDoubleArray.wrap(new double[] {99});   // creates a wrapper over the single-element array
         * }</pre>
         *
         * @param a the double array to wrap; must not be {@code null}
         * @return a new DisposableDoubleArray wrapping the given array
         * @throws IllegalArgumentException if {@code a} is {@code null}
         */
        public static DisposableDoubleArray wrap(final double[] a) {
            return new DisposableDoubleArray(a);
        }

        /**
         * Returns the double value at the specified index.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray arr = DisposableDoubleArray.wrap(new double[] {1, 2, 3});
         * arr.get(0);    // returns 1
         * arr.get(2);    // returns 3
         * arr.get(-1);   // throws ArrayIndexOutOfBoundsException
         * arr.get(3);    // throws ArrayIndexOutOfBoundsException
         * }</pre>
         *
         * @param index the index of the element to return
         * @return the double value at the specified index
         * @throws ArrayIndexOutOfBoundsException if the index is out of range
         */
        public double get(final int index) {
            return a[index];
        }

        /**
         * Returns the length of the wrapped double array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray.wrap(new double[] {1, 2, 3}).length();   // returns 3
         * DisposableDoubleArray.wrap(new double[0]).length();            // returns 0
         * DisposableDoubleArray.create(10).length();                     // returns 10
         * DisposableDoubleArray.wrap(new double[] {99}).length();        // returns 1
         * }</pre>
         *
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a new double array containing the same elements as the wrapped array.
         * Use this method when the data must be retained or modified, since the wrapped
         * array may be reused by the producer and must not be stored. The returned array
         * is independent of this instance.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray arr = DisposableDoubleArray.wrap(new double[] {1, 2, 3});
         * arr.copy();                                             // returns [1, 2, 3]
         * DisposableDoubleArray.wrap(new double[0]).copy();       // returns empty double[]
         * DisposableDoubleArray.wrap(new double[] {99}).copy();   // returns [99]
         * arr.copy() != arr.copy();                               // true (independent copies)
         * }</pre>
         *
         * @return a new double array containing copies of the elements
         */
        public double[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the double array to a Double object array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray arr = DisposableDoubleArray.wrap(new double[] {1, 2, 3});
         * arr.box();                                             // returns [Double.valueOf(1), Double.valueOf(2), Double.valueOf(3)]
         * DisposableDoubleArray.wrap(new double[0]).box();       // returns empty Double[]
         * DisposableDoubleArray.wrap(new double[] {99}).box();   // returns [Double.valueOf(99)]
         * arr.box().length;                                      // returns 3
         * }</pre>
         *
         * @return a new Double array containing boxed values
         */
        public Double[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a DoubleList.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray arr = DisposableDoubleArray.wrap(new double[] {1, 2, 3});
         * arr.toList();                                             // returns DoubleList [1, 2, 3]
         * DisposableDoubleArray.wrap(new double[0]).toList();       // returns empty DoubleList
         * DisposableDoubleArray.wrap(new double[] {99}).toList();   // returns DoubleList [99]
         * arr.toList().size();                                      // returns 3
         * }</pre>
         *
         * @return a new DoubleList containing the array elements
         */
        public DoubleList toList() {
            return DoubleList.of(copy());
        }

        /**
         * Converts the array to a Collection of the specified type.
         * Each double value is boxed to Double.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray arr = DisposableDoubleArray.wrap(new double[] {1, 2, 3});
         * arr.toCollection(ArrayList::new);                                             // returns [1, 2, 3]
         * DisposableDoubleArray.wrap(new double[0]).toCollection(ArrayList::new);       // returns empty list
         * DisposableDoubleArray.wrap(new double[] {99}).toCollection(ArrayList::new);   // returns [99]
         * arr.toCollection(HashSet::new);                                               // returns Set containing 1, 2, 3
         * }</pre>
         *
         * @param <C> the type of the collection to create
         * @param supplier a function that creates a new collection instance with the specified capacity
         * @return a new collection containing the boxed array elements
         */
        public <C extends Collection<Double>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (final double e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         * Calculates the sum of all double values in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray.wrap(new double[] {1, 2, 3}).sum();   // returns 6.0
         * DisposableDoubleArray.wrap(new double[0]).sum();            // returns 0
         * DisposableDoubleArray.wrap(new double[] {5}).sum();         // returns 5
         * DisposableDoubleArray.wrap(new double[] {-1, 1}).sum();     // returns 0
         * }</pre>
         *
         * @return the sum of all elements
         */
        public double sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all double values in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray.wrap(new double[] {1, 2, 3}).average();   // returns 2.0
         * DisposableDoubleArray.wrap(new double[0]).average();            // returns 0.0
         * DisposableDoubleArray.wrap(new double[] {5}).average();         // returns 5.0
         * DisposableDoubleArray.wrap(new double[] {1, 3}).average();      // returns 2.0
         * }</pre>
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum double value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray.wrap(new double[] {3, 1, 2}).min();    // returns 1
         * DisposableDoubleArray.wrap(new double[] {5}).min();          // returns 5
         * DisposableDoubleArray.wrap(new double[0]).min();             // throws IllegalArgumentException
         * DisposableDoubleArray.wrap(new double[] {-1, 0, 1}).min();   // returns -1
         * }</pre>
         *
         * @return the minimum value
         * @throws IllegalArgumentException if the array is empty
         */
        public double min() {
            return N.min(a);
        }

        /**
         * Finds the maximum double value in the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray.wrap(new double[] {1, 3, 2}).max();    // returns 3
         * DisposableDoubleArray.wrap(new double[] {5}).max();          // returns 5
         * DisposableDoubleArray.wrap(new double[0]).max();             // throws IllegalArgumentException
         * DisposableDoubleArray.wrap(new double[] {-1, 0, 1}).max();   // returns 1
         * }</pre>
         *
         * @return the maximum value
         * @throws IllegalArgumentException if the array is empty
         */
        public double max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray arr = DisposableDoubleArray.wrap(new double[] {1, 2, 3});
         * List<Double> collected = new ArrayList<>();
         * arr.foreach(collected::add);                                             // collected contains [1, 2, 3]
         * DisposableDoubleArray.wrap(new double[0]).foreach(e -> {});              // invokes nothing for the empty array
         * DisposableDoubleArray.wrap(new double[] {99}).foreach(collected::add);   // adds 99
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed for each element
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void foreach(final Throwables.DoubleConsumer<E> action) throws E {
            for (final double e : a) {
                action.accept(e);
            }
        }

        /**
         * Applies the given function to the wrapped array and returns the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray arr = DisposableDoubleArray.wrap(new double[] {1, 2, 3});
         * arr.apply(a -> a.length);                                             // returns 3
         * arr.apply(a -> (int) a[0]);                                           // returns 1
         * DisposableDoubleArray.wrap(new double[0]).apply(a -> a.length);       // returns 0
         * DisposableDoubleArray.wrap(new double[] {99}).apply(a -> a.length);   // returns 1
         * }</pre>
         *
         * @param <R> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the function to apply to the array
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super double[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         * Performs the given action with the wrapped array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray arr = DisposableDoubleArray.wrap(new double[] {1, 2, 3});
         * arr.accept(a -> System.out.println(a.length));                                         // prints 3
         * DisposableDoubleArray.wrap(new double[0]).accept(a -> System.out.println(a.length));   // prints 0
         * DisposableDoubleArray.wrap(new double[] {99}).accept(a -> System.out.println(a[0]));   // prints 99
         * arr.accept(a -> {});                                                                   // invokes a no-op consumer
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to perform with the array
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super double[], E> action) throws E {
            action.accept(a);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray arr = DisposableDoubleArray.wrap(new double[] {1, 2, 3});
         * arr.join(", ");                                             // returns "1, 2, 3"
         * DisposableDoubleArray.wrap(new double[0]).join(", ");       // returns ""
         * DisposableDoubleArray.wrap(new double[] {99}).join(", ");   // returns "99"
         * arr.join("-");                                              // returns "1-2-3"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @return a string containing the joined elements
         */
        public String join(final String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         * Joins the string representations of the array elements using the specified delimiter,
         * prefix, and suffix.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray arr = DisposableDoubleArray.wrap(new double[] {1, 2, 3});
         * arr.join(", ", "[", "]");                                             // returns "[1, 2, 3]"
         * DisposableDoubleArray.wrap(new double[0]).join(", ", "[", "]");       // returns "[]"
         * DisposableDoubleArray.wrap(new double[] {99}).join(", ", "[", "]");   // returns "[99]"
         * arr.join("-", "{", "}");                                              // returns "{1-2-3}"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the wrapped double array.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDoubleArray arr = DisposableDoubleArray.wrap(new double[] {1, 2, 3});
         * arr.toString();                                             // returns string representation of [1, 2, 3]
         * DisposableDoubleArray.wrap(new double[0]).toString();       // returns string representation of empty array
         * DisposableDoubleArray.wrap(new double[] {99}).toString();   // returns string representation of [99]
         * }</pre>
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped double array directly, without copying.
         * The returned array must not be cached or modified; use {@link #copy()} when an
         * independent array is required.
         *
         * @return the wrapped double array (the live backing array, not a copy)
         */
        protected double[] values() {
            return a;
        }
    }

    /**
     * A wrapper class for Deque that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of a Deque with utility methods for accessing
     * and transforming the data. The Deque itself should never be cached or modified externally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Deque<String> deque = new ArrayDeque<>();
     * deque.add("first");
     * deque.add("second");
     * DisposableDeque<String> disposable = DisposableDeque.wrap(deque);
     * String first = disposable.getFirst();      // "first"
     * List<String> list = disposable.toList();   // creates a new list
     * }</pre>
     *
     * @param <T> the type of elements in the deque
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableDeque<T> implements NoCachingNoUpdating {

        /** The deque. */
        private final Deque<T> deque;

        /**
         * Constructs a DisposableDeque wrapping the specified deque.
         *
         * @param deque the deque to wrap; must not be {@code null}
         */
        protected DisposableDeque(final Deque<T> deque) {
            N.checkArgNotNull(deque, cs.deque);
            this.deque = deque;
        }

        /**
         * Creates a new DisposableDeque backed by a new ArrayDeque with the specified initial capacity.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableDeque<String> deque = DisposableDeque.create(5);    // creates an empty deque with capacity 5
         * DisposableDeque.create(0);                                    // creates an empty deque with capacity 0
         * DisposableDeque.create(-1);                                   // throws IllegalArgumentException
         * DisposableDeque.create(10);                                   // creates an empty deque with capacity 10
         * }</pre>
         *
         * @param <T> the type of elements in the deque
         * @param len the initial capacity of the deque; must be non-negative
         * @return a new DisposableDeque instance
         * @throws IllegalArgumentException if {@code len} is negative
         */
        public static <T> DisposableDeque<T> create(final int len) {
            if (len < 0) {
                throw new IllegalArgumentException("Length must be non-negative: " + len);
            }
            return new DisposableDeque<>(new ArrayDeque<>(len));
        }

        /**
         * Wraps an existing Deque in a DisposableDeque.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
         * DisposableDeque<String> deque = DisposableDeque.wrap(data);
         * deque.size();                                           // returns 3
         * DisposableDeque.wrap(new ArrayDeque<>());               // creates a wrapper over the empty deque
         * DisposableDeque.wrap(null);                             // throws IllegalArgumentException
         * DisposableDeque.wrap(new ArrayDeque<>(List.of("x")));   // creates a wrapper over the single-element deque
         * }</pre>
         *
         * @param <T> the type of elements in the deque
         * @param deque the deque to wrap; must not be {@code null}
         * @return a new DisposableDeque wrapping the given deque
         * @throws IllegalArgumentException if {@code deque} is {@code null}
         */
        public static <T> DisposableDeque<T> wrap(final Deque<T> deque) {
            return new DisposableDeque<>(deque);
        }

        /**
         * Returns the number of elements in the deque.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
         * DisposableDeque<String> deque = DisposableDeque.wrap(data);
         * deque.size();                                      // returns 3
         * DisposableDeque.wrap(new ArrayDeque<>()).size();   // returns 0
         * }</pre>
         *
         * @return the size of the deque
         */
        public int size() {
            return deque.size();
        }

        /**
         * Retrieves the first element of the deque.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
         * DisposableDeque<String> deque = DisposableDeque.wrap(data);
         * deque.getFirst();   // returns "a"
         * DisposableDeque<String> empty = DisposableDeque.wrap(new ArrayDeque<>());
         * empty.getFirst();   // throws NoSuchElementException
         * }</pre>
         *
         * @return the first element of the deque
         * @throws NoSuchElementException if the deque is empty
         */
        public T getFirst() {
            return deque.getFirst();
        }

        /**
         * Retrieves the last element of the deque.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
         * DisposableDeque<String> deque = DisposableDeque.wrap(data);
         * deque.getLast();   // returns "c"
         * DisposableDeque<String> empty = DisposableDeque.wrap(new ArrayDeque<>());
         * empty.getLast();   // throws NoSuchElementException
         * }</pre>
         *
         * @return the last element of the deque
         * @throws NoSuchElementException if the deque is empty
         */
        public T getLast() {
            return deque.getLast();
        }

        /**
         * Returns an array containing all elements of the deque.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
         * DisposableDeque<String> deque = DisposableDeque.wrap(data);
         * deque.toArray(new String[0]);   // returns ["a", "b", "c"]
         * deque.toArray(new String[5]);   // returns ["a", "b", "c", null, null] (padded)
         * }</pre>
         *
         * If the supplied array is large enough, the elements are stored in it; otherwise a new
         * array of the same runtime type is allocated. Follows the same contract as
         * {@link java.util.Collection#toArray(Object[])}.
         * The returned array is safe to cache and modify.
         *
         * @param <A> the runtime type of the target array
         * @param a the array into which the elements are to be stored if it is large enough;
         *          otherwise a new array of the same runtime type is allocated
         * @return an array containing all elements from the deque
         * @throws ArrayStoreException if the runtime type of {@code a} is not a supertype of the element type
         * @throws NullPointerException if {@code a} is {@code null}
         */
        public <A> A[] toArray(final A[] a) {
            return deque.toArray(a);
        }

        /**
         * Converts the deque to a List.
         * The returned list is a new instance and can be safely cached or modified.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
         * DisposableDeque<String> deque = DisposableDeque.wrap(data);
         * deque.toList();                                      // returns ["a", "b", "c"]
         * DisposableDeque.wrap(new ArrayDeque<>()).toList();   // returns []
         * }</pre>
         *
         * @return a new List containing all elements from the deque
         */
        public List<T> toList() {
            return new ArrayList<>(deque);
        }

        /**
         * Converts the deque to a Set.
         * The returned set is a new instance and can be safely cached or modified.
         * Duplicate elements in the deque will appear only once in the set.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "a"));
         * DisposableDeque<String> deque = DisposableDeque.wrap(data);
         * deque.toSet();                                      // returns Set containing "a", "b" (duplicates removed)
         * DisposableDeque.wrap(new ArrayDeque<>()).toSet();   // returns empty Set
         * }</pre>
         *
         * @return a new Set containing the unique elements from the deque
         */
        public Set<T> toSet() {
            return new HashSet<>(deque);
        }

        /**
         * Converts the deque to a Collection of the specified type.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
         * DisposableDeque<String> deque = DisposableDeque.wrap(data);
         * deque.toCollection(ArrayList::new);                                      // returns ["a", "b", "c"]
         * DisposableDeque.wrap(new ArrayDeque<>()).toCollection(ArrayList::new);   // returns empty list
         * }</pre>
         *
         * @param <C> the type of the collection to create
         * @param supplier a function that creates a new collection instance with the specified capacity
         * @return a new collection containing all elements from the deque
         */
        public <C extends Collection<T>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(size());
            result.addAll(deque);
            return result;
        }

        /**
         * Performs the given action for each element of the deque.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
         * DisposableDeque<String> deque = DisposableDeque.wrap(data);
         * List<String> collected = new ArrayList<>();
         * deque.foreach(collected::add);                               // collected contains ["a", "b", "c"]
         * DisposableDeque.wrap(new ArrayDeque<>()).foreach(e -> {});   // invokes nothing for the empty deque
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to be performed for each element
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void foreach(final Throwables.Consumer<? super T, E> action) throws E {
            for (final T e : deque) {
                action.accept(e);
            }
        }

        /**
         * Applies the given function to the wrapped deque and returns the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
         * DisposableDeque<String> deque = DisposableDeque.wrap(data);
         * deque.apply(Deque::size);                                      // returns 3
         * DisposableDeque.wrap(new ArrayDeque<>()).apply(Deque::size);   // returns 0
         * }</pre>
         *
         * @param <R> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the function to apply to the deque
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super Deque<T>, ? extends R, E> func) throws E {
            return func.apply(deque);
        }

        /**
         * Performs the given action with the wrapped deque.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
         * DisposableDeque<String> deque = DisposableDeque.wrap(data);
         * deque.accept(d -> System.out.println(d.size()));                                      // prints 3
         * DisposableDeque.wrap(new ArrayDeque<>()).accept(d -> System.out.println(d.size()));   // prints 0
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to perform with the deque
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super Deque<T>, E> action) throws E {
            action.accept(deque);
        }

        /**
         * Joins the string representations of the deque elements using the specified delimiter.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
         * DisposableDeque<String> deque = DisposableDeque.wrap(data);
         * deque.join(", ");                                      // returns "a, b, c"
         * DisposableDeque.wrap(new ArrayDeque<>()).join(", ");   // returns ""
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @return a string containing the joined elements
         */
        public String join(final String delimiter) {
            return Strings.join(deque, delimiter);
        }

        /**
         * Joins the string representations of the deque elements using the specified delimiter,
         * prefix, and suffix.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
         * DisposableDeque<String> deque = DisposableDeque.wrap(data);
         * deque.join(", ", "[", "]");                                      // returns "[a, b, c]"
         * DisposableDeque.wrap(new ArrayDeque<>()).join(", ", "[", "]");   // returns "[]"
         * }</pre>
         *
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(deque, delimiter, prefix, suffix);
        }

        @Override
        public String toString() {
            return N.toString(deque);
        }
    }

    /**
     * A wrapper class for Map.Entry that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of a Map.Entry. The entry itself should never
     * be cached, and setValue() is not supported.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map.Entry<String, Integer> entry = Map.entry("key", 100);
     * DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
     * String key = disposable.getKey();                      // "key"
     * Integer value = disposable.getValue();                 // 100
     * Map.Entry<String, Integer> copy = disposable.copy();   // creates a mutable copy
     * }</pre>
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     */
    @Beta
    @SequentialOnly
    @Stateful
    abstract class DisposableEntry<K, V> implements Map.Entry<K, V>, NoCachingNoUpdating {

        /**
         * Protected constructor for subclasses.
         */
        protected DisposableEntry() {
        }

        /**
         * Wraps an existing Map.Entry in a DisposableEntry.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map.Entry<String, Integer> entry = Map.entry("key", 100);
         * DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
         * disposable.getKey();                       // returns "key"
         * DisposableEntry.wrap(null);                // throws IllegalArgumentException
         * DisposableEntry.wrap(Map.entry("x", 1));   // creates a wrapper over the single entry
         * }</pre>
         *
         * @param <K> the type of the key
         * @param <V> the type of the value
         * @param entry the Map.Entry to wrap
         * @return a new DisposableEntry wrapping the given entry
         * @throws IllegalArgumentException if the entry is {@code null}
         */
        public static <K, V> DisposableEntry<K, V> wrap(final Map.Entry<K, V> entry) throws IllegalArgumentException {
            N.checkArgNotNull(entry, cs.entry);

            return new DisposableEntry<>() {
                private final Map.Entry<K, V> e = entry;

                @Override
                public K getKey() {
                    return e.getKey();
                }

                @Override
                public V getValue() {
                    return e.getValue();
                }
            };
        }

        /**
         * This operation is not supported as DisposableEntry is read-only.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map.Entry<String, Integer> entry = Map.entry("key", 100);
         * DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
         * disposable.setValue(200);   // throws UnsupportedOperationException
         * }</pre>
         *
         * @param value the new value (ignored)
         * @return never returns
         * @throws UnsupportedOperationException always thrown
         * @deprecated DisposableEntry is immutable
         */
        @Deprecated
        @Override
        public V setValue(final V value) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * Creates a mutable copy of this entry.
         * The returned entry can be safely cached and modified.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map.Entry<String, Integer> entry = Map.entry("key", 100);
         * DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
         * Map.Entry<String, Integer> copy = disposable.copy();   // returns mutable copy with key="key", value=100
         * copy.setValue(200);                                    // copy is mutable so this succeeds
         * disposable.copy().getKey();                            // returns "key"
         * }</pre>
         *
         * @return a new mutable Map.Entry with the same key and value
         */
        public Map.Entry<K, V> copy() {
            return new AbstractMap.SimpleEntry<>(getKey(), getValue());
        }

        /**
         * Applies the given function to this entry and returns the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map.Entry<String, Integer> entry = Map.entry("key", 100);
         * DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
         * disposable.apply(e -> e.getKey() + "=" + e.getValue());             // returns "key=100"
         * DisposableEntry.wrap(Map.entry("x", 1)).apply(e -> e.getValue());   // returns 1
         * }</pre>
         *
         * @param <R> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the function to apply to this entry
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super DisposableEntry<K, V>, ? extends R, E> func) throws E {
            return func.apply(this);
        }

        /**
         * Applies the given bi-function to the key and value of this entry and returns the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableEntry<String, Integer> entry = DisposableEntry.wrap(Map.entry("age", 25));
         * String result = entry.apply((k, v) -> k + "=" + v);   // "age=25"
         * }</pre>
         *
         * @param <R> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the bi-function to apply to the key and value
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <R, E extends Exception> R apply(final Throwables.BiFunction<? super K, ? super V, ? extends R, E> func) throws E {
            return func.apply(getKey(), getValue());
        }

        /**
         * Performs the given action with this entry.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map.Entry<String, Integer> entry = Map.entry("key", 100);
         * DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
         * disposable.accept(e -> System.out.println(e.getKey()));    // prints "key"
         * DisposableEntry.wrap(Map.entry("x", 1)).accept(e -> {});   // invokes a no-op consumer
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the action to perform with this entry
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super DisposableEntry<K, V>, E> action) throws E {
            action.accept(this);
        }

        /**
         * Performs the given bi-consumer action with the key and value of this entry.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map.Entry<String, Integer> entry = Map.entry("key", 100);
         * DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
         * disposable.accept((k, v) -> System.out.println(k + "=" + v));   // prints "key=100"
         * DisposableEntry.wrap(Map.entry("x", 1)).accept((k, v) -> {});   // invokes a no-op consumer
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the bi-consumer action to perform with the key and value
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.BiConsumer<? super K, ? super V, E> action) throws E {
            action.accept(getKey(), getValue());
        }

        /**
         * Returns a string representation of this entry in the form "key=value".
         *
         * @return a string representation of this entry
         */
        @Override
        public String toString() {
            return N.toString(getKey()) + "=" + N.toString(getValue());
        }
    }

    /**
     * A wrapper class for Pair that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of a Pair. The pair itself should never be cached.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("left", 100);
     * DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
     * String left = disposable.left();                  // "left"
     * Integer right = disposable.right();               // 100
     * Pair<String, Integer> copy = disposable.copy();   // creates a mutable copy
     * }</pre>
     *
     * @param <L> the type of the left element
     * @param <R> the type of the right element
     */
    @Beta
    @SequentialOnly
    @Stateful
    abstract class DisposablePair<L, R> implements NoCachingNoUpdating {

        /**
         * Protected constructor for subclasses.
         */
        protected DisposablePair() {
        }

        /**
         * Wraps an existing Pair in a DisposablePair.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Pair<String, Integer> pair = Pair.of("left", 100);
         * DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
         * disposable.left();                      // returns "left"
         * DisposablePair.wrap(null);              // throws IllegalArgumentException
         * DisposablePair.wrap(Pair.of("x", 1));   // creates a wrapper over the single pair
         * }</pre>
         *
         * @param <L> the type of the left element
         * @param <R> the type of the right element
         * @param p the Pair to wrap
         * @return a new DisposablePair wrapping the given pair
         * @throws IllegalArgumentException if the pair is {@code null}
         */
        public static <L, R> DisposablePair<L, R> wrap(final Pair<L, R> p) throws IllegalArgumentException {
            N.checkArgNotNull(p, cs.pair);

            return new DisposablePair<>() {
                private final Pair<L, R> pair = p;

                @Override
                public L left() {
                    return pair.left();
                }

                @Override
                public R right() {
                    return pair.right();
                }
            };
        }

        /**
         * Returns the left element of the pair.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Pair<String, Integer> pair = Pair.of("left", 100);
         * DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
         * disposable.left();                             // returns "left"
         * DisposablePair.wrap(Pair.of("x", 1)).left();   // returns "x"
         * }</pre>
         *
         * @return the left element
         */
        public abstract L left();

        /**
         * Returns the right element of the pair.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Pair<String, Integer> pair = Pair.of("left", 100);
         * DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
         * disposable.right();                             // returns 100
         * DisposablePair.wrap(Pair.of("x", 1)).right();   // returns 1
         * }</pre>
         *
         * @return the right element
         */
        public abstract R right();

        /**
         * Creates a mutable copy of this pair.
         * The returned pair can be safely cached and modified.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Pair<String, Integer> pair = Pair.of("left", 100);
         * DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
         * Pair<String, Integer> copy = disposable.copy();   // returns Pair("left", 100)
         * copy.setRight(200);                               // copy is mutable so this succeeds
         * disposable.copy().left();                         // returns "left"
         * }</pre>
         *
         * @return a new mutable Pair with the same left and right values
         */
        public Pair<L, R> copy() {
            return Pair.of(left(), right());
        }

        /**
         * Applies the given bi-function to the left and right elements and returns the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposablePair<Integer, Integer> pair = DisposablePair.wrap(Pair.of(3, 4));
         * Integer sum = pair.apply((l, r) -> l + r);   // 7
         * }</pre>
         *
         * @param <U> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the bi-function to apply to the left and right elements
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <U, E extends Exception> U apply(final Throwables.BiFunction<? super L, ? super R, ? extends U, E> func) throws E {
            return func.apply(left(), right());
        }

        /**
         * Performs the given bi-consumer action with the left and right elements.
         *
         * @param <E> the type of exception that the action may throw
         * @param action the bi-consumer action to perform
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.BiConsumer<? super L, ? super R, E> action) throws E {
            action.accept(left(), right());
        }

        /**
         * Returns a string representation of this pair in the form "[left, right]".
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Pair<String, Integer> pair = Pair.of("left", 100);
         * DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
         * disposable.toString();                             // returns "[left, 100]"
         * DisposablePair.wrap(Pair.of("x", 1)).toString();   // returns "[x, 1]"
         * }</pre>
         *
         * @return a string representation of this pair
         */
        @Override
        public String toString() {
            return "[" + N.toString(left()) + ", " + N.toString(right()) + "]";
            // return N.toString(left()) + "=" + N.toString(right());
        }
    }

    /**
     * A wrapper class for Triple that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of a Triple. The triple itself should never be cached.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("left", 100, true);
     * DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
     * String left = disposable.left();                             // "left"
     * Integer middle = disposable.middle();                        // 100
     * Boolean right = disposable.right();                          // true
     * Triple<String, Integer, Boolean> copy = disposable.copy();   // creates a mutable copy
     * }</pre>
     *
     * @param <L> the type of the left element
     * @param <M> the type of the middle element
     * @param <R> the type of the right element
     */
    @Beta
    @SequentialOnly
    @Stateful
    abstract class DisposableTriple<L, M, R> implements NoCachingNoUpdating {

        /**
         * Protected constructor for subclasses.
         */
        protected DisposableTriple() {
        }

        /**
         * Wraps an existing Triple in a DisposableTriple.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Triple<String, Integer, Boolean> triple = Triple.of("left", 100, true);
         * DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
         * disposable.left();                                 // returns "left"
         * DisposableTriple.wrap(null);                       // throws IllegalArgumentException
         * DisposableTriple.wrap(Triple.of("x", 1, false));   // creates a wrapper over the single triple
         * }</pre>
         *
         * @param <L> the type of the left element
         * @param <M> the type of the middle element
         * @param <R> the type of the right element
         * @param p the Triple to wrap
         * @return a new DisposableTriple wrapping the given triple
         * @throws IllegalArgumentException if the triple is {@code null}
         */
        public static <L, M, R> DisposableTriple<L, M, R> wrap(final Triple<L, M, R> p) throws IllegalArgumentException {
            N.checkArgNotNull(p, cs.triple);

            return new DisposableTriple<>() {
                private final Triple<L, M, R> triple = p;

                @Override
                public L left() {
                    return triple.left();
                }

                @Override
                public M middle() {
                    return triple.middle();
                }

                @Override
                public R right() {
                    return triple.right();
                }
            };
        }

        /**
         * Returns the left element of the triple.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Triple<String, Integer, Boolean> triple = Triple.of("left", 100, true);
         * DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
         * disposable.left();                                        // returns "left"
         * DisposableTriple.wrap(Triple.of("x", 1, false)).left();   // returns "x"
         * }</pre>
         *
         * @return the left element
         */
        public abstract L left();

        /**
         * Returns the middle element of the triple.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Triple<String, Integer, Boolean> triple = Triple.of("left", 100, true);
         * DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
         * disposable.middle();                                        // returns 100
         * DisposableTriple.wrap(Triple.of("x", 1, false)).middle();   // returns 1
         * }</pre>
         *
         * @return the middle element
         */
        public abstract M middle();

        /**
         * Returns the right element of the triple.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Triple<String, Integer, Boolean> triple = Triple.of("left", 100, true);
         * DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
         * disposable.right();                                        // returns true
         * DisposableTriple.wrap(Triple.of("x", 1, false)).right();   // returns false
         * }</pre>
         *
         * @return the right element
         */
        public abstract R right();

        /**
         * Creates a mutable copy of this triple.
         * The returned triple can be safely cached and modified.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Triple<String, Integer, Boolean> triple = Triple.of("left", 100, true);
         * DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
         * Triple<String, Integer, Boolean> copy = disposable.copy();   // returns Triple("left", 100, true)
         * disposable.copy().left();                                    // returns "left"
         * }</pre>
         *
         * @return a new mutable Triple with the same left, middle, and right values
         */
        public Triple<L, M, R> copy() {
            return Triple.of(left(), middle(), right());
        }

        /**
         * Applies the given tri-function to the left, middle, and right elements and returns the result.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DisposableTriple<Integer, Integer, Integer> triple = DisposableTriple.wrap(Triple.of(1, 2, 3));
         * Integer sum = triple.apply((l, m, r) -> l + m + r);   // 6
         * }</pre>
         *
         * @param <U> the type of the result
         * @param <E> the type of exception that the function may throw
         * @param func the tri-function to apply to the elements
         * @return the result of applying the function
         * @throws E if the function throws an exception
         */
        public <U, E extends Exception> U apply(final Throwables.TriFunction<? super L, ? super M, ? super R, ? extends U, E> func) throws E {
            return func.apply(left(), middle(), right());
        }

        /**
         * Performs the given tri-consumer action with the left, middle, and right elements.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Triple<String, Integer, Boolean> triple = Triple.of("left", 100, true);
         * DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
         * disposable.accept((l, m, r) -> System.out.println(l + "=" + m + "=" + r));   // prints "left=100=true"
         * DisposableTriple.wrap(Triple.of("x", 1, false)).accept((l, m, r) -> {});     // invokes a no-op consumer
         * }</pre>
         *
         * @param <E> the type of exception that the action may throw
         * @param action the tri-consumer action to perform
         * @throws E if the action throws an exception
         */
        public <E extends Exception> void accept(final Throwables.TriConsumer<? super L, ? super M, ? super R, E> action) throws E {
            action.accept(left(), middle(), right());
        }

        /**
         * Returns a string representation of this triple in the form "[left, middle, right]".
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Triple<String, Integer, Boolean> triple = Triple.of("left", 100, true);
         * DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
         * disposable.toString();                                        // returns "[left, 100, true]"
         * DisposableTriple.wrap(Triple.of("x", 1, false)).toString();   // returns "[x, 1, false]"
         * }</pre>
         *
         * @return a string representation of this triple
         */
        @Override
        public String toString() {
            return "[" + N.toString(left()) + ", " + N.toString(middle()) + ", " + N.toString(right()) + "]";
        }
    }

    /**
     * A container class that associates a value with a timestamp.
     * This class is designed for temporal data where the time of creation or modification
     * is important. Like other NoCachingNoUpdating implementations, instances should not
     * be cached or updated directly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Timed<String> timedValue = Timed.of("Hello", System.currentTimeMillis());
     * String value = timedValue.value();    // "Hello"
     * long time = timedValue.timestamp();   // timestamp is in milliseconds
     * }</pre>
     *
     * @param <T> the type of the value
     */
    class Timed<T> implements NoCachingNoUpdating {

        /** The wrapped value */
        protected T value;

        /** The timestamp in milliseconds */
        protected long timeInMillis;

        /**
         * Constructs a Timed instance with the specified value and timestamp.
         *
         * @param value the value to wrap
         * @param timeInMillis the timestamp in milliseconds
         */
        protected Timed(final T value, final long timeInMillis) {
            this.value = value;
            this.timeInMillis = timeInMillis;
        }

        /**
         * Creates a new Timed instance with the specified value and timestamp.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Timed<String> timed = Timed.of("Hello", 12345L);
         * Timed.of(null, 0L);          // null value is allowed
         * Timed.of("value", 0L);       // timestamp is zero
         * Timed.of("value", -1L);      // timestamp is negative
         * }</pre>
         *
         * @param <T> the type of the value
         * @param value the value to wrap
         * @param timeInMillis the timestamp in milliseconds
         * @return a new Timed instance
         */
        public static <T> Timed<T> of(final T value, final long timeInMillis) {
            return new Timed<>(value, timeInMillis);
        }

        /**
         * Updates the value and timestamp of this instance.
         * This method is protected to prevent external modification.
         *
         * @param value the value to set
         * @param timeInMillis the timestamp in milliseconds to set
         */
        protected void set(final T value, final long timeInMillis) {
            this.value = value;
            this.timeInMillis = timeInMillis;
        }

        /**
         * Returns the wrapped value.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Timed<String> timed = Timed.of("Hello", 12345L);
         * timed.value();                // returns "Hello"
         * Timed.of(null, 0L).value();   // returns null
         * Timed.of("x", 1L).value();    // returns "x"
         * }</pre>
         *
         * @return the wrapped value
         */
        public T value() {
            return value;
        }

        /**
         * Returns the timestamp associated with the value.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Timed<String> timed = Timed.of("Hello", 12345L);
         * timed.timestamp();                   // returns 12345L
         * Timed.of("value", 0L).timestamp();   // returns 0L
         * Timed.of("value", -1L).timestamp();  // returns -1L
         * }</pre>
         *
         * @return the timestamp in milliseconds
         */
        public long timestamp() {
            return timeInMillis;
        }

        /**
         * Returns the hash code of this Timed instance.
         * The hash code is computed based on both the timestamp and the value.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Timed<String> t1 = Timed.of("value", 12345L);
         * Timed<String> t2 = Timed.of("value", 12345L);
         * t1.hashCode() == t2.hashCode();   // returns true
         * Timed.of(null, 0L).hashCode();    // returns valid hash code
         * }</pre>
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            int result = Long.hashCode(timeInMillis);
            result = 31 * result + (value == null ? 0 : value.hashCode());
            return result;
        }

        /**
         * Compares this Timed instance with another object for equality.
         * Two Timed instances are equal if they have the same timestamp and value.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Timed<String> t1 = Timed.of("value", 12345L);
         * Timed<String> t2 = Timed.of("value", 12345L);
         * t1.equals(t2);                          // returns true
         * t1.equals(Timed.of("other", 12345L));   // returns false
         * t1.equals(null);                        // returns false
         * t1.equals("not a Timed");               // returns false
         * }</pre>
         *
         * @param obj the object to compare with
         * @return {@code true} if the objects are equal, {@code false} otherwise
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Timed<?> other) {
                return timeInMillis == other.timeInMillis && N.equals(value, other.value);
            }

            return false;
        }

        /**
         * Returns a string representation of this Timed instance in the form "timestamp: value".
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Timed<String> timed = Timed.of("Hello", 12345L);
         * timed.toString();                // returns "12345: Hello"
         * Timed.of(null, 0L).toString();   // returns "0: null"
         * Timed.of("x", 1L).toString();    // returns "1: x"
         * }</pre>
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return timeInMillis + ": " + N.toString(value);
        }
    }
}

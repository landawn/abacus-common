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
 * To persist or modify the object, call the {@code copy()} method to create a mutable copy.
 * 
 * <p>This pattern is useful for objects that wrap temporary resources or provide read-only views
 * of data that should not be retained in memory.</p>
 *
 * @since 1.0
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
     * <p>Usage example:</p>
     * <pre>{@code
     * String[] array = {"a", "b", "c"};
     * DisposableArray<String> disposable = DisposableArray.wrap(array);
     * String first = disposable.get(0); // "a"
     * List<String> list = disposable.toList(); // Creates a new list
     * }</pre>
     *
     * @param <T> the type of elements in the array
     * @since 1.0
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
         * The array must not be null.
         *
         * @param a the array to wrap
         * @throws IllegalArgumentException if the array is null
         */
        protected DisposableArray(final T[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableArray with a new array of the specified component type and length.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * DisposableArray<String> array = DisposableArray.create(String.class, 10);
         * }</pre>
         *
         * @param <T> the type of elements in the array
         * @param componentType the class of the array elements
         * @param len the length of the array
         * @return a new DisposableArray instance
         */
        public static <T> DisposableArray<T> create(final Class<T> componentType, final int len) {
            return new DisposableArray<>(N.newArray(componentType, len));
        }

        /**
         * Wraps an existing array in a DisposableArray.
         * The array is not copied; the DisposableArray provides a view of the original array.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Integer[] numbers = {1, 2, 3, 4, 5};
         * DisposableArray<Integer> disposable = DisposableArray.wrap(numbers);
         * }</pre>
         *
         * @param <T> the type of elements in the array
         * @param a the array to wrap
         * @return a new DisposableArray wrapping the given array
         */
        public static <T> DisposableArray<T> wrap(final T[] a) {
            return new DisposableArray<>(a);
        }

        /**
         * Returns the element at the specified index.
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
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Copies the elements to the specified array.
         * If the target array is too small, a new array of the same type is allocated.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * DisposableArray<String> disposable = DisposableArray.wrap(new String[]{"a", "b", "c"});
         * String[] copy = disposable.toArray(new String[0]);
         * }</pre>
         *
         * @param <A> the type of the target array
         * @param target the array into which the elements are to be stored
         * @return an array containing the elements
         */
        public <A> A[] toArray(A[] target) {
            if (target.length < length()) {
                target = N.newArray(target.getClass().getComponentType(), length());
            }

            N.copy(a, 0, target, 0, length());

            return target;
        }

        /**
         * Creates a copy of the wrapped array.
         * This method should be used when you need to cache or modify the array data.
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
         * @return a new Set containing the unique array elements
         */
        public Set<T> toSet() {
            return N.toSet(a);
        }

        /**
         * Converts the array to a Collection of the specified type.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * DisposableArray<String> disposable = DisposableArray.wrap(new String[]{"a", "b", "c"});
         * LinkedList<String> list = disposable.toCollection(LinkedList::new);
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
         * <p>Example:</p>
         * <pre>{@code
         * DisposableArray<String> disposable = DisposableArray.wrap(new String[]{"a", "b", "c"});
         * disposable.foreach(System.out::println); // Prints each element
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
         * <p>Example:</p>
         * <pre>{@code
         * DisposableArray<Integer> disposable = DisposableArray.wrap(new Integer[]{1, 2, 3});
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
         * <p>Example:</p>
         * <pre>{@code
         * DisposableArray<String> disposable = DisposableArray.wrap(new String[]{"a", "b", "c"});
         * String joined = disposable.join(", "); // "a, b, c"
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
         * <p>Example:</p>
         * <pre>{@code
         * DisposableArray<String> disposable = DisposableArray.wrap(new String[]{"a", "b", "c"});
         * String joined = disposable.join(", ", "[", "]"); // "[a, b, c]"
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
         * Returns an iterator over the elements in the array.
         *
         * @return an iterator over the array elements
         */
        @Override
        public Iterator<T> iterator() {
            return ObjIterator.of(a);
        }

        /**
         * Returns a string representation of the array.
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped array.
         * This method is protected to prevent external access to the mutable array.
         *
         * @return the wrapped array
         */
        protected T[] values() {
            return a;
        }
    }

    /**
     * A specialized DisposableArray for Object arrays.
     * This class provides optimized handling for arrays of type Object[].
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * DisposableObjArray array = DisposableObjArray.create(10);
     * // or
     * Object[] objects = {1, "hello", true};
     * DisposableObjArray wrapped = DisposableObjArray.wrap(objects);
     * }</pre>
     *
     * @since 1.0
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableObjArray extends DisposableArray<Object> {

        /**
         * Constructs a DisposableObjArray with the specified Object array.
         *
         * @param a the Object array to wrap
         */
        protected DisposableObjArray(final Object[] a) {
            super(a);
        }

        /**
         * Creates a new DisposableObjArray with a new Object array of the specified length.
         *
         * @param len the length of the array
         * @return a new DisposableObjArray instance
         */
        public static DisposableObjArray create(final int len) {
            return new DisposableObjArray(new Object[len]);
        }

        /**
         * This method is not supported for DisposableObjArray.
         * Use the parameterless create(int) method instead.
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
         * @param a the Object array to wrap
         * @return a new DisposableObjArray wrapping the given array
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
     * <p>Usage example:</p>
     * <pre>{@code
     * boolean[] array = {true, false, true};
     * DisposableBooleanArray disposable = DisposableBooleanArray.wrap(array);
     * boolean first = disposable.get(0); // true
     * BooleanList list = disposable.toList(); // Creates a new list
     * }</pre>
     *
     * @since 1.0
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableBooleanArray implements NoCachingNoUpdating {

        /** The element array */
        private final boolean[] a;

        /**
         * Constructs a DisposableBooleanArray with the specified boolean array.
         *
         * @param a the boolean array to wrap
         * @throws IllegalArgumentException if the array is null
         */
        protected DisposableBooleanArray(final boolean[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableBooleanArray with a new boolean array of the specified length.
         *
         * @param len the length of the array
         * @return a new DisposableBooleanArray instance
         */
        public static DisposableBooleanArray create(final int len) {
            return new DisposableBooleanArray(new boolean[len]);
        }

        /**
         * Wraps an existing boolean array in a DisposableBooleanArray.
         *
         * @param a the boolean array to wrap
         * @return a new DisposableBooleanArray wrapping the given array
         */
        public static DisposableBooleanArray wrap(final boolean[] a) {
            return new DisposableBooleanArray(a);
        }

        /**
         * Returns the boolean value at the specified index.
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
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a copy of the wrapped boolean array.
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
         * @return a new Boolean array containing boxed values
         */
        public Boolean[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a BooleanList.
         * The returned list is a new instance and can be safely cached or modified.
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
         * <p>Example:</p>
         * <pre>{@code
         * DisposableBooleanArray array = DisposableBooleanArray.wrap(new boolean[]{true, false});
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
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the array.
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped boolean array.
         *
         * @return the wrapped array
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
     * <p>Usage example:</p>
     * <pre>{@code
     * char[] array = {'a', 'b', 'c'};
     * DisposableCharArray disposable = DisposableCharArray.wrap(array);
     * char first = disposable.get(0); // 'a'
     * int sum = disposable.sum(); // Sum of char values
     * }</pre>
     *
     * @since 1.0
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableCharArray implements NoCachingNoUpdating {

        /** The element array */
        private final char[] a;

        /**
         * Constructs a DisposableCharArray with the specified char array.
         *
         * @param a the char array to wrap
         * @throws IllegalArgumentException if the array is null
         */
        protected DisposableCharArray(final char[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableCharArray with a new char array of the specified length.
         *
         * @param len the length of the array
         * @return a new DisposableCharArray instance
         */
        public static DisposableCharArray create(final int len) {
            return new DisposableCharArray(new char[len]);
        }

        /**
         * Wraps an existing char array in a DisposableCharArray.
         *
         * @param a the char array to wrap
         * @return a new DisposableCharArray wrapping the given array
         */
        public static DisposableCharArray wrap(final char[] a) {
            return new DisposableCharArray(a);
        }

        /**
         * Returns the char value at the specified index.
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
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a copy of the wrapped char array.
         *
         * @return a new char array containing copies of the elements
         */
        public char[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the char array to a Character object array.
         *
         * @return a new Character array containing boxed values
         */
        public Character[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a CharList.
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
         * @return the sum of all elements
         */
        public int sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all char values in the array.
         * The char values are treated as their numeric Unicode values.
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum char value in the array.
         *
         * @return the minimum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public char min() {
            return N.min(a);
        }

        /**
         * Finds the maximum char value in the array.
         *
         * @return the maximum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public char max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
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
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the array.
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped char array.
         *
         * @return the wrapped array
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
     * <p>Usage example:</p>
     * <pre>{@code
     * byte[] array = {1, 2, 3, 4, 5};
     * DisposableByteArray disposable = DisposableByteArray.wrap(array);
     * byte first = disposable.get(0); // 1
     * int sum = disposable.sum(); // 15
     * }</pre>
     *
     * @since 1.0
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableByteArray implements NoCachingNoUpdating {

        /** The element array */
        private final byte[] a;

        /**
         * Constructs a DisposableByteArray with the specified byte array.
         *
         * @param a the byte array to wrap
         * @throws IllegalArgumentException if the array is null
         */
        protected DisposableByteArray(final byte[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableByteArray with a new byte array of the specified length.
         *
         * @param len the length of the array
         * @return a new DisposableByteArray instance
         */
        public static DisposableByteArray create(final int len) {
            return new DisposableByteArray(new byte[len]);
        }

        /**
         * Wraps an existing byte array in a DisposableByteArray.
         *
         * @param a the byte array to wrap
         * @return a new DisposableByteArray wrapping the given array
         */
        public static DisposableByteArray wrap(final byte[] a) {
            return new DisposableByteArray(a);
        }

        /**
         * Returns the byte value at the specified index.
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
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a copy of the wrapped byte array.
         *
         * @return a new byte array containing copies of the elements
         */
        public byte[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the byte array to a Byte object array.
         *
         * @return a new Byte array containing boxed values
         */
        public Byte[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a ByteList.
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
         * @return the sum of all elements
         */
        public int sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all byte values in the array.
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum byte value in the array.
         *
         * @return the minimum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public byte min() {
            return N.min(a);
        }

        /**
         * Finds the maximum byte value in the array.
         *
         * @return the maximum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public byte max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
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
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the array.
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped byte array.
         *
         * @return the wrapped array
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
     * <p>Usage example:</p>
     * <pre>{@code
     * short[] array = {100, 200, 300};
     * DisposableShortArray disposable = DisposableShortArray.wrap(array);
     * short first = disposable.get(0); // 100
     * int sum = disposable.sum(); // 600
     * }</pre>
     *
     * @since 1.0
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableShortArray implements NoCachingNoUpdating {

        /** The element array */
        private final short[] a;

        /**
         * Constructs a DisposableShortArray with the specified short array.
         *
         * @param a the short array to wrap
         * @throws IllegalArgumentException if the array is null
         */
        protected DisposableShortArray(final short[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableShortArray with a new short array of the specified length.
         *
         * @param len the length of the array
         * @return a new DisposableShortArray instance
         */
        public static DisposableShortArray create(final int len) {
            return new DisposableShortArray(new short[len]);
        }

        /**
         * Wraps an existing short array in a DisposableShortArray.
         *
         * @param a the short array to wrap
         * @return a new DisposableShortArray wrapping the given array
         */
        public static DisposableShortArray wrap(final short[] a) {
            return new DisposableShortArray(a);
        }

        /**
         * Returns the short value at the specified index.
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
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a copy of the wrapped short array.
         *
         * @return a new short array containing copies of the elements
         */
        public short[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the short array to a Short object array.
         *
         * @return a new Short array containing boxed values
         */
        public Short[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a ShortList.
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
         * @return the sum of all elements
         */
        public int sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all short values in the array.
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum short value in the array.
         *
         * @return the minimum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public short min() {
            return N.min(a);
        }

        /**
         * Finds the maximum short value in the array.
         *
         * @return the maximum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public short max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
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
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the array.
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped short array.
         *
         * @return the wrapped array
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
     * <p>Usage example:</p>
     * <pre>{@code
     * int[] array = {10, 20, 30, 40, 50};
     * DisposableIntArray disposable = DisposableIntArray.wrap(array);
     * int first = disposable.get(0); // 10
     * int sum = disposable.sum(); // 150
     * double avg = disposable.average(); // 30.0
     * }</pre>
     *
     * @since 1.0
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableIntArray implements NoCachingNoUpdating {

        /** The element array */
        private final int[] a;

        /**
         * Constructs a DisposableIntArray with the specified int array.
         *
         * @param a the int array to wrap
         * @throws IllegalArgumentException if the array is null
         */
        protected DisposableIntArray(final int[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableIntArray with a new int array of the specified length.
         *
         * @param len the length of the array
         * @return a new DisposableIntArray instance
         */
        public static DisposableIntArray create(final int len) {
            return new DisposableIntArray(new int[len]);
        }

        /**
         * Wraps an existing int array in a DisposableIntArray.
         *
         * @param a the int array to wrap
         * @return a new DisposableIntArray wrapping the given array
         */
        public static DisposableIntArray wrap(final int[] a) {
            return new DisposableIntArray(a);
        }

        /**
         * Returns the int value at the specified index.
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
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a copy of the wrapped int array.
         *
         * @return a new int array containing copies of the elements
         */
        public int[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the int array to an Integer object array.
         *
         * @return a new Integer array containing boxed values
         */
        public Integer[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to an IntList.
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
         * @return the sum of all elements
         */
        public int sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all int values in the array.
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum int value in the array.
         *
         * @return the minimum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public int min() {
            return N.min(a);
        }

        /**
         * Finds the maximum int value in the array.
         *
         * @return the maximum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public int max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
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
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the array.
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped int array.
         *
         * @return the wrapped array
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
     * <p>Usage example:</p>
     * <pre>{@code
     * long[] array = {1000L, 2000L, 3000L};
     * DisposableLongArray disposable = DisposableLongArray.wrap(array);
     * long first = disposable.get(0); // 1000L
     * long sum = disposable.sum(); // 6000L
     * }</pre>
     *
     * @since 1.0
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableLongArray implements NoCachingNoUpdating {

        /** The element array */
        private final long[] a;

        /**
         * Constructs a DisposableLongArray with the specified long array.
         *
         * @param a the long array to wrap
         * @throws IllegalArgumentException if the array is null
         */
        protected DisposableLongArray(final long[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableLongArray with a new long array of the specified length.
         *
         * @param len the length of the array
         * @return a new DisposableLongArray instance
         */
        public static DisposableLongArray create(final int len) {
            return new DisposableLongArray(new long[len]);
        }

        /**
         * Wraps an existing long array in a DisposableLongArray.
         *
         * @param a the long array to wrap
         * @return a new DisposableLongArray wrapping the given array
         */
        public static DisposableLongArray wrap(final long[] a) {
            return new DisposableLongArray(a);
        }

        /**
         * Returns the long value at the specified index.
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
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a copy of the wrapped long array.
         *
         * @return a new long array containing copies of the elements
         */
        public long[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the long array to a Long object array.
         *
         * @return a new Long array containing boxed values
         */
        public Long[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a LongList.
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
         * @return the sum of all elements
         */
        public long sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all long values in the array.
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum long value in the array.
         *
         * @return the minimum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public long min() {
            return N.min(a);
        }

        /**
         * Finds the maximum long value in the array.
         *
         * @return the maximum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public long max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
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
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the array.
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped long array.
         *
         * @return the wrapped array
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
     * <p>Usage example:</p>
     * <pre>{@code
     * float[] array = {1.5f, 2.5f, 3.5f};
     * DisposableFloatArray disposable = DisposableFloatArray.wrap(array);
     * float first = disposable.get(0); // 1.5f
     * float sum = disposable.sum(); // 7.5f
     * }</pre>
     *
     * @since 1.0
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableFloatArray implements NoCachingNoUpdating {

        /** The element array */
        private final float[] a;

        /**
         * Constructs a DisposableFloatArray with the specified float array.
         *
         * @param a the float array to wrap
         * @throws IllegalArgumentException if the array is null
         */
        protected DisposableFloatArray(final float[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableFloatArray with a new float array of the specified length.
         *
         * @param len the length of the array
         * @return a new DisposableFloatArray instance
         */
        public static DisposableFloatArray create(final int len) {
            return new DisposableFloatArray(new float[len]);
        }

        /**
         * Wraps an existing float array in a DisposableFloatArray.
         *
         * @param a the float array to wrap
         * @return a new DisposableFloatArray wrapping the given array
         */
        public static DisposableFloatArray wrap(final float[] a) {
            return new DisposableFloatArray(a);
        }

        /**
         * Returns the float value at the specified index.
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
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a copy of the wrapped float array.
         *
         * @return a new float array containing copies of the elements
         */
        public float[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the float array to a Float object array.
         *
         * @return a new Float array containing boxed values
         */
        public Float[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a FloatList.
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
         * @return the sum of all elements
         */
        public float sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all float values in the array.
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum float value in the array.
         *
         * @return the minimum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public float min() {
            return N.min(a);
        }

        /**
         * Finds the maximum float value in the array.
         *
         * @return the maximum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public float max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
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
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the array.
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped float array.
         *
         * @return the wrapped array
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
     * <p>Usage example:</p>
     * <pre>{@code
     * double[] array = {1.1, 2.2, 3.3, 4.4};
     * DisposableDoubleArray disposable = DisposableDoubleArray.wrap(array);
     * double first = disposable.get(0); // 1.1
     * double sum = disposable.sum(); // 11.0
     * double avg = disposable.average(); // 2.75
     * }</pre>
     *
     * @since 1.0
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableDoubleArray implements NoCachingNoUpdating {

        /** The element array */
        private final double[] a;

        /**
         * Constructs a DisposableDoubleArray with the specified double array.
         *
         * @param a the double array to wrap
         * @throws IllegalArgumentException if the array is null
         */
        protected DisposableDoubleArray(final double[] a) {
            N.checkArgNotNull(a, cs.a);
            this.a = a;
        }

        /**
         * Creates a new DisposableDoubleArray with a new double array of the specified length.
         *
         * @param len the length of the array
         * @return a new DisposableDoubleArray instance
         */
        public static DisposableDoubleArray create(final int len) {
            return new DisposableDoubleArray(new double[len]);
        }

        /**
         * Wraps an existing double array in a DisposableDoubleArray.
         *
         * @param a the double array to wrap
         * @return a new DisposableDoubleArray wrapping the given array
         */
        public static DisposableDoubleArray wrap(final double[] a) {
            return new DisposableDoubleArray(a);
        }

        /**
         * Returns the double value at the specified index.
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
         * @return the length of the array
         */
        public int length() {
            return a.length;
        }

        /**
         * Creates a copy of the wrapped double array.
         *
         * @return a new double array containing copies of the elements
         */
        public double[] copy() { //NOSONAR
            return N.clone(a);
        }

        /**
         * Converts the double array to a Double object array.
         *
         * @return a new Double array containing boxed values
         */
        public Double[] box() {
            return Array.box(a);
        }

        /**
         * Converts the array to a DoubleList.
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
         * @return the sum of all elements
         */
        public double sum() {
            return N.sum(a);
        }

        /**
         * Calculates the average of all double values in the array.
         *
         * @return the average of all elements, or 0 if the array is empty
         */
        public double average() {
            return N.average(a);
        }

        /**
         * Finds the minimum double value in the array.
         *
         * @return the minimum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public double min() {
            return N.min(a);
        }

        /**
         * Finds the maximum double value in the array.
         *
         * @return the maximum value
         * @throws ArrayIndexOutOfBoundsException if the array is empty
         */
        public double max() {
            return N.max(a);
        }

        /**
         * Performs the given action for each element of the array.
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
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the array.
         *
         * @return a string representation of the array
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         * Returns the wrapped double array.
         *
         * @return the wrapped array
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
     * <p>Usage example:</p>
     * <pre>{@code
     * Deque<String> deque = new ArrayDeque<>();
     * deque.add("first");
     * deque.add("second");
     * DisposableDeque<String> disposable = DisposableDeque.wrap(deque);
     * String first = disposable.getFirst(); // "first"
     * List<String> list = disposable.toList(); // Creates a new list
     * }</pre>
     *
     * @param <T> the type of elements in the deque
     * @since 1.0
     */
    @Beta
    @SequentialOnly
    @Stateful
    class DisposableDeque<T> implements NoCachingNoUpdating {

        /** The deque. */
        private final Deque<T> deque;

        /**
         * Constructs a DisposableDeque with the specified deque.
         *
         * @param deque the deque to wrap
         * @throws IllegalArgumentException if the deque is null
         */
        protected DisposableDeque(final Deque<T> deque) {
            N.checkArgNotNull(deque, cs.deque);
            this.deque = deque;
        }

        /**
         * Creates a new DisposableDeque with a new ArrayDeque of the specified initial capacity.
         *
         * @param <T> the type of elements in the deque
         * @param len the initial capacity of the deque
         * @return a new DisposableDeque instance
         */
        public static <T> DisposableDeque<T> create(final int len) {
            return new DisposableDeque<>(new ArrayDeque<>(len));
        }

        /**
         * Wraps an existing Deque in a DisposableDeque.
         *
         * @param <T> the type of elements in the deque
         * @param deque the deque to wrap
         * @return a new DisposableDeque wrapping the given deque
         */
        public static <T> DisposableDeque<T> wrap(final Deque<T> deque) {
            return new DisposableDeque<>(deque);
        }

        /**
         * Returns the number of elements in the deque.
         *
         * @return the size of the deque
         */
        public int size() {
            return deque.size();
        }

        /**
         * Retrieves the first element of the deque.
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
         * @return the last element of the deque
         * @throws NoSuchElementException if the deque is empty
         */
        public T getLast() {
            return deque.getLast();
        }

        /**
         * Copies all elements from the deque to the specified array.
         * If the array is too small, a new array of the same type is allocated.
         *
         * @param <A> the type of the array elements
         * @param a the array into which the elements are to be stored
         * @return an array containing all elements from the deque
         */
        public <A> A[] toArray(final A[] a) {
            return deque.toArray(a);
        }

        /**
         * Converts the deque to a List.
         * The returned list is a new instance and can be safely cached or modified.
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
         * @return a new Set containing the unique elements from the deque
         */
        public Set<T> toSet() {
            return new HashSet<>(deque);
        }

        /**
         * Converts the deque to a Collection of the specified type.
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
         * @param delimiter the delimiter to use between elements
         * @param prefix the prefix to prepend to the result
         * @param suffix the suffix to append to the result
         * @return a string containing the joined elements with prefix and suffix
         */
        public String join(final String delimiter, final String prefix, final String suffix) {
            return Strings.join(deque, delimiter, prefix, suffix);
        }

        /**
         * Returns a string representation of the deque.
         *
         * @return a string representation of the deque
         */
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
     * <p>Usage example:</p>
     * <pre>{@code
     * Map.Entry<String, Integer> entry = Map.entry("key", 100);
     * DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
     * String key = disposable.getKey(); // "key"
     * Integer value = disposable.getValue(); // 100
     * Map.Entry<String, Integer> copy = disposable.copy(); // Creates a mutable copy
     * }</pre>
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @since 1.0
     */
    @Beta
    @SequentialOnly
    @Stateful
    abstract class DisposableEntry<K, V> implements Map.Entry<K, V>, NoCachingNoUpdating {

        /**
         * Wraps an existing Map.Entry in a DisposableEntry.
         *
         * @param <K> the type of the key
         * @param <V> the type of the value
         * @param entry the Map.Entry to wrap
         * @return a new DisposableEntry wrapping the given entry
         * @throws IllegalArgumentException if the entry is null
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
         * @return a new mutable Map.Entry with the same key and value
         */
        public Map.Entry<K, V> copy() {
            return new AbstractMap.SimpleEntry<>(getKey(), getValue());
        }

        /**
         * Applies the given function to this entry and returns the result.
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
         * <p>Example:</p>
         * <pre>{@code
         * DisposableEntry<String, Integer> entry = DisposableEntry.wrap(Map.entry("age", 25));
         * String result = entry.apply((k, v) -> k + "=" + v); // "age=25"
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
            return getKey() + "=" + getValue();
        }
    }

    /**
     * A wrapper class for Pair that enforces no-caching and no-updating semantics.
     * This class provides a read-only view of a Pair. The pair itself should never be cached.
     * 
     * <p>Usage example:</p>
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("left", 100);
     * DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
     * String left = disposable.left(); // "left"
     * Integer right = disposable.right(); // 100
     * Pair<String, Integer> copy = disposable.copy(); // Creates a mutable copy
     * }</pre>
     *
     * @param <L> the type of the left element
     * @param <R> the type of the right element
     * @since 1.0
     */
    @Beta
    @SequentialOnly
    @Stateful
    abstract class DisposablePair<L, R> implements NoCachingNoUpdating {

        /**
         * Wraps an existing Pair in a DisposablePair.
         *
         * @param <L> the type of the left element
         * @param <R> the type of the right element
         * @param p the Pair to wrap
         * @return a new DisposablePair wrapping the given pair
         * @throws IllegalArgumentException if the pair is null
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
         * @return the left element
         */
        public abstract L left();

        /**
         * Returns the right element of the pair.
         *
         * @return the right element
         */
        public abstract R right();

        /**
         * Creates a mutable copy of this pair.
         * The returned pair can be safely cached and modified.
         *
         * @return a new mutable Pair with the same left and right values
         */
        public Pair<L, R> copy() {
            return Pair.of(left(), right());
        }

        /**
         * Applies the given bi-function to the left and right elements and returns the result.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * DisposablePair<Integer, Integer> pair = DisposablePair.wrap(Pair.of(3, 4));
         * Integer sum = pair.apply((l, r) -> l + r); // 7
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
     * <p>Usage example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("left", 100, true);
     * DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
     * String left = disposable.left(); // "left"
     * Integer middle = disposable.middle(); // 100
     * Boolean right = disposable.right(); // true
     * Triple<String, Integer, Boolean> copy = disposable.copy(); // Creates a mutable copy
     * }</pre>
     *
     * @param <L> the type of the left element
     * @param <M> the type of the middle element
     * @param <R> the type of the right element
     * @since 1.0
     */
    @Beta
    @SequentialOnly
    @Stateful
    abstract class DisposableTriple<L, M, R> implements NoCachingNoUpdating {

        /**
         * Wraps an existing Triple in a DisposableTriple.
         *
         * @param <L> the type of the left element
         * @param <M> the type of the middle element
         * @param <R> the type of the right element
         * @param p the Triple to wrap
         * @return a new DisposableTriple wrapping the given triple
         * @throws IllegalArgumentException if the triple is null
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
         * @return the left element
         */
        public abstract L left();

        /**
         * Returns the middle element of the triple.
         *
         * @return the middle element
         */
        public abstract M middle();

        /**
         * Returns the right element of the triple.
         *
         * @return the right element
         */
        public abstract R right();

        /**
         * Creates a mutable copy of this triple.
         * The returned triple can be safely cached and modified.
         *
         * @return a new mutable Triple with the same left, middle, and right values
         */
        public Triple<L, M, R> copy() {
            return Triple.of(left(), middle(), right());
        }

        /**
         * Applies the given tri-function to the left, middle, and right elements and returns the result.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * DisposableTriple<Integer, Integer, Integer> triple = DisposableTriple.wrap(Triple.of(1, 2, 3));
         * Integer sum = triple.apply((l, m, r) -> l + m + r); // 6
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
     * <p>Usage example:</p>
     * <pre>{@code
     * Timed<String> timedValue = Timed.of("Hello", System.currentTimeMillis());
     * String value = timedValue.value(); // "Hello"
     * long time = timedValue.timestamp(); // The timestamp in milliseconds
     * }</pre>
     *
     * @param <T> the type of the value
     * @since 1.0
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
         * @param value the new value
         * @param timeInMillis the new timestamp in milliseconds
         */
        protected void set(final T value, final long timeInMillis) {
            this.value = value;
            this.timeInMillis = timeInMillis;
        }

        /**
         * Returns the wrapped value.
         *
         * @return the value
         */
        public T value() {
            return value;
        }

        /**
         * Returns the timestamp associated with the value.
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
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return (int) (timeInMillis * 31 + (value == null ? 0 : value.hashCode()));
        }

        /**
         * Compares this Timed instance with another object for equality.
         * Two Timed instances are equal if they have the same timestamp and value.
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
         * @return a string representation
         */
        @Override
        public String toString() {
            return timeInMillis + ": " + N.toString(value);
        }
    }
}
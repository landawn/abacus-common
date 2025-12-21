/*
 * Copyright (C) 2020 HaiYang Li
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

import java.util.function.Consumer;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.stream.Stream;

/**
 * An immutable wrapper around an array that provides read-only access to its elements.
 * Once created, the contents of an ImmutableArray cannot be modified, making it thread-safe
 * and suitable for use as a defensive copy of array data.
 * 
 * <p>This class implements {@link Iterable} to allow for enhanced for-loop iteration and
 * provides various utility methods for accessing and searching array elements. It also
 * supports conversion to {@link ImmutableList} and {@link Stream} for functional operations.</p>
 * 
 * <p>Note: While the ImmutableArray itself cannot be modified, if it contains mutable objects,
 * those objects themselves can still be modified. For {@code true} immutability, ensure that the
 * array contains only immutable objects.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ImmutableArray<String> array = ImmutableArray.of("apple", "banana", "cherry");
 * String first = array.get(0);   // "apple"
 * boolean hasApple = array.contains("apple");   // true
 * 
 * // Iterate over elements
 * for (String fruit : array) {
 *     System.out.println(fruit);
 * }
 * }</pre>
 *
 * @param <T> the type of elements in this array
 * @see ImmutableList
 * @see Immutable
 */
@com.landawn.abacus.annotation.Immutable
public final class ImmutableArray<T> implements Iterable<T>, Immutable {
    private final T[] elements;
    private final int length;

    ImmutableArray(final T[] elements) {
        this.elements = elements == null ? (T[]) N.EMPTY_OBJECT_ARRAY : elements;
        length = N.len(this.elements);
    }

    /**
     * Creates an ImmutableArray containing a single element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> single = ImmutableArray.of("hello");
     * }</pre>
     *
     * @param <T> the type of the element
     * @param e1 the element to be stored in the array
     * @return an ImmutableArray containing the specified element
     */
    public static <T> ImmutableArray<T> of(final T e1) {
        return new ImmutableArray<>(N.asArray(e1));
    }

    /**
     * Creates an ImmutableArray containing two elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<Integer> pair = ImmutableArray.of(1, 2);
     * }</pre>
     *
     * @param <T> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @return an ImmutableArray containing the specified elements in order
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2) {
        return new ImmutableArray<>(N.asArray(e1, e2));
    }

    /**
     * Creates an ImmutableArray containing three elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> triple = ImmutableArray.of("a", "b", "c");
     * }</pre>
     *
     * @param <T> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @return an ImmutableArray containing the specified elements in order
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3));
    }

    /**
     * Creates an ImmutableArray containing four elements.
     *
     * @param <T> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @return an ImmutableArray containing the specified elements in order
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4));
    }

    /**
     * Creates an ImmutableArray containing five elements.
     *
     * @param <T> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @return an ImmutableArray containing the specified elements in order
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5));
    }

    /**
     * Creates an ImmutableArray containing six elements.
     *
     * @param <T> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @return an ImmutableArray containing the specified elements in order
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5, e6));
    }

    /**
     * Creates an ImmutableArray containing seven elements.
     *
     * @param <T> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @return an ImmutableArray containing the specified elements in order
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5, e6, e7));
    }

    /**
     * Creates an ImmutableArray containing eight elements.
     *
     * @param <T> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @param e8 the eighth element
     * @return an ImmutableArray containing the specified elements in order
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7, final T e8) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5, e6, e7, e8));
    }

    /**
     * Creates an ImmutableArray containing nine elements.
     *
     * @param <T> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @param e8 the eighth element
     * @param e9 the ninth element
     * @return an ImmutableArray containing the specified elements in order
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7, final T e8, final T e9) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5, e6, e7, e8, e9));
    }

    /**
     * Creates an ImmutableArray containing ten elements.
     *
     * @param <T> the type of the elements
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @param e8 the eighth element
     * @param e9 the ninth element
     * @param e10 the tenth element
     * @return an ImmutableArray containing the specified elements in order
     */
    public static <T> ImmutableArray<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7, final T e8, final T e9,
            final T e10) {
        return new ImmutableArray<>(N.asArray(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));
    }

    /**
     * Creates an ImmutableArray by making a defensive copy of the specified array.
     * Changes to the original array after this call will not affect the returned
     * ImmutableArray.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] original = {"a", "b", "c"};
     * ImmutableArray<String> immutable = ImmutableArray.copyOf(original);
     * original[0] = "modified";  // Does not affect immutable array
     * }</pre>
     *
     * @param <T> the type of the elements
     * @param elements the array whose elements are to be copied
     * @return an ImmutableArray containing a copy of the specified array's elements,
     *         or an empty ImmutableArray if the input is null
     */
    public static <T> ImmutableArray<T> copyOf(final T[] elements) {
        return new ImmutableArray<>(elements == null ? null : elements.clone());
    }

    /**
     * Wraps the provided array into an ImmutableArray without copying. Changes to the
     * specified array will be reflected in the returned ImmutableArray, which violates
     * the immutability contract.
     * 
     * <p><strong>Warning:</strong> This method should be used with extreme caution and only
     * when you can guarantee that the provided array will not be modified after wrapping.
     * In most cases, use {@link #copyOf(Object[])} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] original = {"a", "b", "c"};
     * ImmutableArray<String> wrapped = ImmutableArray.wrap(original);
     * original[0] = "modified";  // This WILL affect the "immutable" array!
     * }</pre>
     *
     * @param <T> the type of the elements
     * @param elements the array to be wrapped
     * @return an ImmutableArray backed by the specified array
     * @deprecated the ImmutableArray may be modified through the specified {@code elements}
     * 
     * <p>Example (showing the danger):</p>
     */
    @Deprecated
    @Beta
    public static <T> ImmutableArray<T> wrap(final T[] elements) {
        return new ImmutableArray<>(elements);
    }

    /**
     * Returns the number of elements in this array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
     * int len = array.length();   // returns 3
     * }</pre>
     *
     * @return the length of this array
     */
    public int length() {
        return length;
    }

    /**
     * Returns {@code true} if this array contains no elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> empty = ImmutableArray.copyOf(new String[0]);
     * boolean isEmpty = empty.isEmpty();   // returns true
     * }</pre>
     *
     * @return {@code true} if this array has length 0, {@code false} otherwise
     */
    public boolean isEmpty() {
        return length == 0;
    }

    /**
     * Returns the element at the specified position in this array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
     * String second = array.get(1);   // returns "b"
     * }</pre>
     *
     * @param index the index of the element to return (zero-based)
     * @return the element at the specified position
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     *         (index &lt; 0 || index &gt;= length())
     */
    public T get(final int index) {
        return elements[index];
    }

    /**
     * Returns the index of the first occurrence of the specified element in this array,
     * or -1 if this array does not contain the element. The comparison uses the
     * {@code equals} method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> array = ImmutableArray.of("a", "b", "c", "b");
     * int index = array.indexOf("b");      // returns 1
     * int notFound = array.indexOf("d");   // returns -1
     * }</pre>
     *
     * @param valueToFind the element to search for
     * @return the index of the first occurrence of the element, or -1 if not found
     */
    public int indexOf(final T valueToFind) {
        return N.indexOf(elements, valueToFind);
    }

    /**
     * Returns the index of the last occurrence of the specified element in this array,
     * or -1 if this array does not contain the element. The comparison uses the
     * {@code equals} method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> array = ImmutableArray.of("a", "b", "c", "b");
     * int lastIndex = array.lastIndexOf("b");   // returns 3
     * }</pre>
     *
     * @param valueToFind the element to search for
     * @return the index of the last occurrence of the element, or -1 if not found
     */
    public int lastIndexOf(final T valueToFind) {
        return N.lastIndexOf(elements, valueToFind);
    }

    /**
     * Returns {@code true} if this array contains the specified element.
     * The comparison uses the {@code equals} method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
     * boolean hasB = array.contains("b");   // returns true
     * boolean hasD = array.contains("d");   // returns false
     * }</pre>
     *
     * @param valueToFind the element whose presence is to be tested
     * @return {@code true} if this array contains the specified element
     */
    public boolean contains(final T valueToFind) {
        return N.contains(elements, valueToFind);
    }

    /**
     * Returns a new ImmutableArray containing the elements from the specified range
     * of this array. The range is half-open: [fromIndex, toIndex).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> array = ImmutableArray.of("a", "b", "c", "d", "e");
     * ImmutableArray<String> subArray = array.copy(1, 4);   // contains ["b", "c", "d"]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a new ImmutableArray containing the specified range of elements
     * @throws IndexOutOfBoundsException if fromIndex &lt; 0, toIndex &gt; length(),
     *         or fromIndex &gt; toIndex
     */
    public ImmutableArray<T> copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, length);

        return new ImmutableArray<>(N.copyOfRange(elements, fromIndex, toIndex));
    }

    /**
     * Returns an immutable view of this array as a list. The returned list is backed
     * by this array, so it reflects the array's contents but cannot be modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
     * ImmutableList<String> list = array.asList();
     * // list.size() == 3, list.get(0).equals("a")
     * }</pre>
     *
     * @return an ImmutableList view of this array
     */
    public ImmutableList<T> asList() {
        return ImmutableList.wrap(N.asList(elements));
    }

    /**
     * Returns an iterator over the elements in this array in proper sequence.
     * The returned iterator does not support the {@code remove} operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
     * ObjIterator<String> it = array.iterator();
     * while (it.hasNext()) {
     *     System.out.println(it.next());
     * }
     * }</pre>
     *
     * @return an iterator over the elements in this array
     */
    @Override
    public ObjIterator<T> iterator() {
        return ObjIterator.of(elements);
    }

    /**
     * Returns a sequential Stream with this array as its source. This method provides
     * a convenient way to perform functional operations on the array's elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> array = ImmutableArray.of("apple", "banana", "cherry");
     * List<String> filtered = array.stream()
     *     .filter(s -> s.startsWith("a"))
     *     .collect(Collectors.toList());   // ["apple"]
     * }</pre>
     *
     * @return a Stream over the elements in this array
     */
    public Stream<T> stream() {
        return Stream.of(elements);
    }

    /**
     * Performs the given action for each element of the array in order.
     * This method is equivalent to:
     * <pre>{@code
     * for (T element : array) {
     *     consumer.accept(element);
     * }
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
     * array.forEach(System.out::println);   // prints each element
     * }</pre>
     *
     * @param consumer the action to be performed for each element
     * @throws IllegalArgumentException if the specified consumer is null
     */
    @Override
    public void forEach(final Consumer<? super T> consumer) throws IllegalArgumentException {
        N.checkArgNotNull(consumer, "consumer"); // NOSONAR

        for (int i = 0; i < length; i++) {
            consumer.accept(elements[i]);
        }
    }

    /**
     * Performs the given action for each element of the array in order.
     * Unlike {@link #forEach(Consumer)}, this method can throw checked exceptions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> array = ImmutableArray.of("file1.txt", "file2.txt");
     * array.foreach(fileName -> {
     *     // This could throw IOException
     *     Files.readAllLines(Paths.get(fileName));
     * });
     * }</pre>
     *
     * @param <E> the type of exception that the consumer may throw
     * @param consumer the action to be performed for each element
     * @throws IllegalArgumentException if the specified consumer is null
     * @throws E if the consumer throws an exception
     */
    @Beta
    public <E extends Exception> void foreach(final Throwables.Consumer<? super T, E> consumer) throws IllegalArgumentException, E { // NOSONAR
        N.checkArgNotNull(consumer, "consumer"); // NOSONAR

        for (int i = 0; i < length; i++) {
            consumer.accept(elements[i]);
        }
    }

    /**
     * Performs the given action for each element of the array, providing both the index
     * and the element to the consumer. This is useful when you need to know the position
     * of each element during iteration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> array = ImmutableArray.of("a", "b", "c");
     * array.foreachIndexed((index, value) -> {
     *     System.out.println(index + ": " + value);
     * });
     * // Output:
     * // 0: a
     * // 1: b
     * // 2: c
     * }</pre>
     *
     * @param <E> the type of exception that the consumer may throw
     * @param consumer a BiConsumer that accepts the index and the element
     * @throws IllegalArgumentException if the specified consumer is null
     * @throws E if the consumer throws an exception
     */
    @Beta
    public <E extends Exception> void foreachIndexed(final Throwables.IntObjConsumer<? super T, E> consumer) throws IllegalArgumentException, E { // NOSONAR
        N.checkArgNotNull(consumer, "consumer"); // NOSONAR

        for (int i = 0; i < length; i++) {
            consumer.accept(i, elements[i]);
        }
    }

    /**
     * Returns a hash code value for this array. The hash code is computed based on
     * the contents of the array, so two ImmutableArrays with the same elements in
     * the same order will have the same hash code.
     *
     * @return a hash code value for this array
     */
    @Override
    public int hashCode() {
        return N.hashCode(elements) * 31;
    }

    /**
     * Compares this ImmutableArray with the specified object for equality.
     * Returns {@code true} if and only if the specified object is also an
     * ImmutableArray and both arrays contain the same elements in the same order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<String> array1 = ImmutableArray.of("a", "b", "c");
     * ImmutableArray<String> array2 = ImmutableArray.of("a", "b", "c");
     * ImmutableArray<String> array3 = ImmutableArray.of("a", "b", "d");
     * 
     * boolean equal1 = array1.equals(array2);   // returns true
     * boolean equal2 = array1.equals(array3);   // returns false
     * }</pre>
     *
     * @param obj the object to be compared for equality with this array
     * @return {@code true} if the specified object is equal to this array
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof ImmutableArray && N.equals(elements, ((ImmutableArray<T>) obj).elements);
    }

    /**
     * Returns a string representation of this array. The string representation
     * consists of the array's elements, enclosed in square brackets ("[]").
     * Adjacent elements are separated by the characters ", " (comma and space).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableArray<Integer> array = ImmutableArray.of(1, 2, 3);
     * String str = array.toString();   // returns "[1, 2, 3]"
     * }</pre>
     *
     * @return a string representation of this array
     */
    @Override
    public String toString() {
        return N.toString(elements);
    }
}

/*
 * Copyright (c) 2015, Haiyang Li.
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

import java.io.Serial;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.function.IntFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteUnaryOperator;
import com.landawn.abacus.util.stream.ByteStream;

/**
 * A resizable array implementation for primitive byte values, providing similar functionality to ArrayList
 * but specifically optimized for byte primitives. This class offers better performance and memory efficiency
 * compared to {@code ArrayList<Byte>} by avoiding the overhead of boxing and unboxing.
 * 
 * <p>This implementation is not thread-safe. If multiple threads access a ByteList instance concurrently,
 * and at least one of the threads modifies the list structurally, it must be synchronized externally.
 * 
 * <p>The class provides various utility methods for common operations such as sorting, searching, 
 * reversing, and mathematical operations, making it suitable for numerical computations and data processing.
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 *
 */
public final class ByteList extends PrimitiveList<Byte, byte[], ByteList> {

    @Serial
    private static final long serialVersionUID = 6361439693114081075L;

    static final Random RAND = new SecureRandom();

    private byte[] elementData = N.EMPTY_BYTE_ARRAY;

    private int size = 0;

    /**
     * Constructs an empty ByteList with an initial capacity of zero.
     * The internal array will be initialized to an empty array and will grow
     * as needed when elements are added.
     */
    public ByteList() {
    }

    /**
     * Constructs an empty ByteList with the specified initial capacity.
     *
     * <p>This constructor is useful when the approximate size of the list is known in advance,
     * as it can help avoid the performance overhead of array resizing during element additions.</p>
     *
     * @param initialCapacity the initial capacity of the list. Must be non-negative.
     * @throws IllegalArgumentException if the specified initial capacity is negative
     * @throws OutOfMemoryError if the requested array size exceeds the maximum array size
     */
    public ByteList(final int initialCapacity) {
        N.checkArgNotNegative(initialCapacity, cs.initialCapacity);

        elementData = initialCapacity == 0 ? N.EMPTY_BYTE_ARRAY : new byte[initialCapacity];
    }

    /**
     * Constructs a ByteList using the specified array as the backing array for this list.
     * The array is used directly without copying, making this constructor very efficient.
     * Changes to the array will be reflected in the list and vice versa.
     * The size of the list will be set to the length of the array.
     *
     * @param a the array to be used as the backing array for this list. Must not be null.
     */
    public ByteList(final byte[] a) {
        this(N.requireNonNull(a), a.length);
    }

    /**
     * Constructs a ByteList using the specified array as the backing array with a specific size.
     * The array is used directly without copying. This constructor allows creating a list
     * that uses only a portion of the provided array. The size parameter must not exceed
     * the length of the array.
     *
     * @param a the array to be used as the backing array for this list. Must not be null.
     * @param size the number of elements from the array to be included in the list.
     *             Must be between 0 and a.length (inclusive).
     * @throws IndexOutOfBoundsException if size is negative or greater than a.length
     */
    public ByteList(final byte[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates a new ByteList containing the specified elements. The specified array is used directly
     * as the backing array without copying, so subsequent modifications to the array will affect the list.
     * If the input array is {@code null}, an empty list is returned.
     *
     * @param a the array of elements to be included in the new list. Can be {@code null}.
     * @return a new ByteList containing the elements from the specified array, or an empty list if the array is {@code null}
     */
    public static ByteList of(final byte... a) {
        return new ByteList(N.nullToEmpty(a));
    }

    /**
     * Creates a new ByteList containing the first {@code size} elements of the specified array.
     * The array is used directly as the backing array without copying for efficiency.
     * If the input array is {@code null}, it is treated as an empty array.
     *
     * @param a the array of byte values to be used as the backing array. Can be {@code null}.
     * @param size the number of elements from the array to include in the list.
     *             Must be between 0 and the array length (inclusive).
     * @return a new ByteList containing the first {@code size} elements of the specified array
     * @throws IndexOutOfBoundsException if {@code size} is negative or greater than the array length
     */
    public static ByteList of(final byte[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new ByteList(N.nullToEmpty(a), size);
    }

    /**
     * Creates a new ByteList that is a copy of the specified array.
     *
     * <p>Unlike {@link #of(byte...)}, this method always creates a defensive copy of the input array,
     * ensuring that modifications to the returned list do not affect the original array.</p>
     *
     * <p>If the input array is {@code null}, an empty list is returned.</p>
     *
     * @param a the array to be copied. Can be {@code null}.
     * @return a new ByteList containing a copy of the elements from the specified array,
     *         or an empty list if the array is {@code null}
     */
    public static ByteList copyOf(final byte[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates a new ByteList that is a copy of the specified range within the given array.
     *
     * <p>This method creates a defensive copy of the elements in the range [fromIndex, toIndex),
     * ensuring that modifications to the returned list do not affect the original array.</p>
     *
     * @param a the array from which a range is to be copied. Must not be {@code null}.
     * @param fromIndex the initial index of the range to be copied, inclusive.
     * @param toIndex the final index of the range to be copied, exclusive.
     * @return a new ByteList containing a copy of the elements in the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > a.length}
     *                                   or {@code fromIndex > toIndex}
     */
    public static ByteList copyOf(final byte[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates a ByteList containing a sequence of byte values from startInclusive to endExclusive.
     * The sequence increases by 1 for each element. If startInclusive equals endExclusive,
     * an empty list is returned. If startInclusive is greater than endExclusive,
     * the sequence decreases by 1 for each element.
     *
     * @param startInclusive the starting value (inclusive) of the sequence
     * @param endExclusive the ending value (exclusive) of the sequence
     * @return a new ByteList containing the sequential values
     */
    public static ByteList range(final byte startInclusive, final byte endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     * Creates a ByteList containing a sequence of byte values from startInclusive to endExclusive,
     * with a specified step between consecutive values. The step can be positive or negative,
     * but must not be zero. If the step is positive, the sequence increases; if negative, it decreases.
     * An empty list is returned if the range cannot be traversed with the given step.
     *
     * @param startInclusive the starting value (inclusive) of the sequence
     * @param endExclusive the ending value (exclusive) of the sequence
     * @param by the step value between consecutive elements. Must not be zero.
     * @return a new ByteList containing the sequential values with the specified step
     * @throws IllegalArgumentException if by is zero
     */
    public static ByteList range(final byte startInclusive, final byte endExclusive, final byte by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     * Creates a ByteList containing a sequence of byte values from startInclusive to endInclusive.
     * Both endpoints are included in the sequence. The sequence increases by 1 for each element.
     * If startInclusive equals endInclusive, a list with a single element is returned.
     * If startInclusive is greater than endInclusive, the sequence decreases by 1 for each element.
     *
     * @param startInclusive the starting value (inclusive) of the sequence
     * @param endInclusive the ending value (inclusive) of the sequence
     * @return a new ByteList containing the sequential values including both endpoints
     */
    public static ByteList rangeClosed(final byte startInclusive, final byte endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     * Creates a ByteList containing a sequence of byte values from startInclusive to endInclusive,
     * with a specified step between consecutive values. Both endpoints are included if reachable.
     * The step can be positive or negative, but must not be zero. The last value in the sequence
     * is the last value that doesn't exceed endInclusive (for positive steps) or doesn't go below
     * endInclusive (for negative steps).
     *
     * @param startInclusive the starting value (inclusive) of the sequence
     * @param endInclusive the ending value (inclusive) of the sequence
     * @param by the step value between consecutive elements. Must not be zero.
     * @return a new ByteList containing the sequential values with the specified step
     * @throws IllegalArgumentException if by is zero
     */
    public static ByteList rangeClosed(final byte startInclusive, final byte endInclusive, final byte by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     * Creates a ByteList containing the specified element repeated a given number of times.
     * This method is useful for initializing lists with a default value.
     * If len is zero or negative, an empty list is returned.
     *
     * @param element the byte value to be repeated
     * @param len the number of times to repeat the element. Must be non-negative.
     * @return a new ByteList containing len copies of the specified element
     * @throws IllegalArgumentException if len is negative
     */
    public static ByteList repeat(final byte element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates a ByteList containing random byte values.
     * Each byte value is randomly generated within the full range of byte values
     * (from Byte.MIN_VALUE to Byte.MAX_VALUE inclusive). The distribution is uniform
     * across all possible byte values.
     *
     * @param len the number of random byte values to generate. Must be non-negative.
     * @return a new ByteList containing len random byte values
     * @throws IllegalArgumentException if len is negative
     */
    public static ByteList random(final int len) {
        final int bound = Byte.MAX_VALUE - Byte.MIN_VALUE + 1;
        final byte[] a = new byte[len];

        // Keep consistent with ByteStream/ShortList/ShortStream/CharList/CharStream.
        // RAND.nextBytes(a);
        for (int i = 0; i < len; i++) {
            a[i] = (byte) (RAND.nextInt(bound) + Byte.MIN_VALUE);
        }

        return of(a);
    }

    /**
     * Returns the underlying byte array backing this list without creating a copy.
     * This method provides direct access to the internal array for performance-critical operations.
     *
     * <p><b>Warning:</b> The returned array is the actual internal storage of this list.
     * Modifications to the returned array will directly affect this list's contents.
     * The array may be larger than the list size; only indices from 0 to size()-1 contain valid elements.
     *
     * <p>This method is marked as {@code @Beta} and should be used with caution.
     *
     * @return the internal byte array backing this list
     * @deprecated This method is deprecated because it exposes internal state and can lead to bugs.
     *             Use {@link #toArray()} instead to get a safe copy of the list elements.
     *             If you need the internal array for performance reasons and understand the risks,
     *             consider using a custom implementation or wrapping this list appropriately.
     */
    @Beta
    @Deprecated
    @Override
    public byte[] array() {
        return elementData;
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index the index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    public byte get(final int index) {
        rangeCheck(index);

        return elementData[index];
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @param index the index of the element to replace
     * @param e the element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    public byte set(final int index, final byte e) {
        rangeCheck(index);

        final byte oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     * Appends the specified element to the end of this list.
     * The list will automatically grow if necessary to accommodate the new element.
     *
     * <p>This method runs in amortized constant time. If the internal array needs to be
     * resized to accommodate the new element, all existing elements will be copied to
     * a new, larger array.</p>
     *
     * @param e the byte value to be appended to this list
     */
    public void add(final byte e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices). The index must be
     * within the range from 0 to size() inclusive.
     *
     * <p>This method runs in linear time in the worst case (when inserting at the beginning
     * of the list), as it may need to shift all existing elements.</p>
     *
     * @param index the index at which the specified element is to be inserted.
     *              Must be between 0 and size() inclusive.
     * @param e the byte value to be inserted
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    public void add(final int index, final byte e) {
        rangeCheckForAdd(index);

        ensureCapacity(size + 1);

        final int numMoved = size - index;

        if (numMoved > 0) {
            N.copy(elementData, index, elementData, index + 1, numMoved);
        }

        elementData[index] = e;

        size++;
    }

    /**
     * Appends all elements from the specified ByteList to the end of this list.
     * The elements are appended in the order they appear in the specified list.
     * If the specified list is empty, this list remains unchanged.
     *
     * @param c the ByteList containing elements to be added to this list. Can be null.
     * @return {@code true} if this list changed as a result of the call (i.e., if any elements were added)
     */
    @Override
    public boolean addAll(final ByteList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        final int numNew = c.size();

        ensureCapacity(size + numNew);

        N.copy(c.array(), 0, elementData, size, numNew);

        size += numNew;

        return true;
    }

    /**
     * Inserts all elements from the specified ByteList into this list at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to the right
     * (increases their indices by the number of elements added). The new elements will appear in this
     * list in the order they appear in the specified list.
     *
     * @param index the index at which to insert the first element from the specified list.
     *              Must be between 0 and size() inclusive.
     * @param c the ByteList containing elements to be inserted into this list. Can be null.
     * @return {@code true} if this list changed as a result of the call (i.e., if any elements were added)
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > size())
     */
    @Override
    public boolean addAll(final int index, final ByteList c) {
        rangeCheckForAdd(index);

        if (N.isEmpty(c)) {
            return false;
        }

        final int numNew = c.size();

        ensureCapacity(size + numNew); // Increments modCount

        final int numMoved = size - index;

        if (numMoved > 0) {
            N.copy(elementData, index, elementData, index + numNew, numMoved);
        }

        N.copy(c.array(), 0, elementData, index, numNew);

        size += numNew;

        return true;
    }

    /**
     * Appends all elements from the specified array to the end of this list.
     * The elements are appended in the order they appear in the array.
     * If the array is null or empty, this list remains unchanged.
     *
     * @param a the array containing elements to be added to this list. Can be null.
     * @return {@code true} if this list changed as a result of the call (i.e., if any elements were added)
     */
    @Override
    public boolean addAll(final byte[] a) {
        return addAll(size(), a);
    }

    /**
     * Inserts all elements from the specified array into this list at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to the right
     * (increases their indices by the length of the array). The new elements will appear in this list
     * in the same order as they appear in the array.
     *
     * @param index the index at which to insert the first element from the array.
     *              Must be between 0 and size() inclusive.
     * @param a the array containing elements to be inserted into this list. Can be null.
     * @return {@code true} if this list changed as a result of the call (i.e., if any elements were added)
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > size())
     */
    @Override
    public boolean addAll(final int index, final byte[] a) {
        rangeCheckForAdd(index);

        if (N.isEmpty(a)) {
            return false;
        }

        final int numNew = a.length;

        ensureCapacity(size + numNew); // Increments modCount

        final int numMoved = size - index;

        if (numMoved > 0) {
            N.copy(elementData, index, elementData, index + numNew, numMoved);
        }

        N.copy(a, 0, elementData, index, numNew);

        size += numNew;

        return true;
    }

    /**
     * Checks if the specified index is valid for an add operation.
     * For add operations, the valid range includes size() to allow appending at the end.
     *
     * @param index the index to check
     * @throws IndexOutOfBoundsException if index > size || index < 0
     */
    private void rangeCheckForAdd(final int index) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    /**
     * Removes the first occurrence of the specified element from this list, if it is present.
     * If the list does not contain the element, it is unchanged. This method scans the list
     * from the beginning and removes the first matching element found.
     *
     * <p>This method runs in linear time, as it may need to search through the entire list
     * to find the element.</p>
     *
     * @param e the byte value to be removed from this list
     * @return {@code true} if this list contained the specified element (and it was removed);
     *         {@code false} otherwise
     */
    public boolean remove(final byte e) {
        for (int i = 0; i < size; i++) {
            if (elementData[i] == e) {

                fastRemove(i);

                return true;
            }
        }

        return false;
    }

    /**
     * Removes all occurrences of the specified element from this list.
     * The list is compacted after removal, preserving the order of the remaining elements.
     * If the list does not contain the element, it is unchanged.
     *
     * @param e the byte value to be removed from this list
     * @return {@code true} if at least one occurrence of the specified element was removed
     */
    public boolean removeAllOccurrences(final byte e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] != e) {
                elementData[w++] = elementData[i];
            }
        }

        final int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, (byte) 0);

            size = w;
        }

        return numRemoved > 0;
    }

    /**
     * Private method to quickly remove an element at a specific index without bounds checking.
     * This method is used internally when the index is known to be valid.
     *
     * @param index the index of the element to remove
     */
    private void fastRemove(final int index) {
        final int numMoved = size - index - 1;

        if (numMoved > 0) {
            N.copy(elementData, index + 1, elementData, index, numMoved);
        }

        elementData[--size] = 0; // clear to let GC do its work
    }

    /**
     * Removes from this list all elements that are contained in the specified ByteList.
     * After this call returns, this list will contain no elements in common with the specified list.
     * The order of the remaining elements is preserved.
     *
     * @param c the ByteList containing elements to be removed from this list. Can be null.
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean removeAll(final ByteList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        return batchRemove(c, false) > 0;
    }

    /**
     * Removes from this list all elements that are contained in the specified array.
     * After this call returns, this list will contain no elements present in the specified array.
     * The order of the remaining elements is preserved.
     *
     * @param a the array containing elements to be removed from this list. Can be null.
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean removeAll(final byte[] a) {
        if (N.isEmpty(a)) {
            return false;
        }

        return removeAll(of(a));
    }

    /**
     * Removes all elements from this list that satisfy the given predicate.
     * Elements are tested in order from first to last, and those for which the predicate
     * returns {@code true} are removed. The order of the remaining elements is preserved.
     *
     * <p>This is a functional programming approach to element removal, allowing complex
     * removal logic to be expressed concisely using lambda expressions or method references.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of((byte)1, (byte)-2, (byte)3, (byte)-4, (byte)5);
     * list.removeIf(b -> b < 0);  // Removes negative values
     * // list now contains: [1, 3, 5]
     * }</pre>
     *
     * @param p the predicate which returns {@code true} for elements to be removed. Must not be null.
     * @return {@code true} if any elements were removed; {@code false} if the list was unchanged
     *
     */
    public boolean removeIf(final BytePredicate p) {
        final ByteList tmp = new ByteList(size());

        for (int i = 0; i < size; i++) {
            if (!p.test(elementData[i])) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, elementData, 0, tmp.size());
        N.fill(elementData, tmp.size(), size, (byte) 0);
        size = tmp.size;

        return true;
    }

    /**
     * Removes duplicate elements from this list, keeping only the first occurrence of each value.
     * If the list is already sorted, this method uses an optimized algorithm.
     * The relative order of distinct elements is preserved.
     *
     * @return {@code true} if any duplicate elements were removed
     */
    @Override
    public boolean removeDuplicates() {
        if (size < 2) {
            return false;
        }

        final boolean isSorted = isSorted();
        int idx = 0;

        if (isSorted) {
            for (int i = 1; i < size; i++) {
                if (elementData[i] != elementData[idx]) {
                    elementData[++idx] = elementData[i];
                }
            }

        } else {
            final Set<Byte> set = N.newLinkedHashSet(size);
            set.add(elementData[0]);

            for (int i = 1; i < size; i++) {
                if (set.add(elementData[i])) {
                    elementData[++idx] = elementData[i];
                }
            }
        }

        if (idx == size - 1) {
            return false;
        } else {
            N.fill(elementData, idx + 1, size, (byte) 0);

            size = idx + 1;
            return true;
        }
    }

    /**
     * Retains only the elements in this list that are contained in the specified ByteList.
     * In other words, removes from this list all elements that are not contained in the specified list.
     * The order of the retained elements is preserved.
     *
     * @param c the ByteList containing elements to be retained in this list. Can be null.
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean retainAll(final ByteList c) {
        if (N.isEmpty(c)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return batchRemove(c, true) > 0;
    }

    /**
     * Retains only the elements in this list that are contained in the specified array.
     * In other words, removes from this list all elements that are not contained in the specified array.
     * The order of the retained elements is preserved.
     *
     * @param a the array containing elements to be retained in this list. Can be null.
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean retainAll(final byte[] a) {
        if (N.isEmpty(a)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(ByteList.of(a));
    }

    /**
     * Helper method for removeAll and retainAll operations.
     * Removes or retains elements based on whether they are contained in the specified list.
     *
     * @param c the list to check against
     * @param complement if true, retains elements in c; if false, removes elements in c
     * @return the number of elements removed
     */
    private int batchRemove(final ByteList c, final boolean complement) {
        final byte[] elementData = this.elementData;//NOSONAR

        int w = 0;

        if (c.size() > 3 && size() > 9) {
            final Set<Byte> set = c.toSet();

            for (int i = 0; i < size; i++) {
                if (set.contains(elementData[i]) == complement) {
                    elementData[w++] = elementData[i];
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (c.contains(elementData[i]) == complement) {
                    elementData[w++] = elementData[i];
                }
            }
        }

        final int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, (byte) 0);

            size = w;
        }

        return numRemoved;
    }

    /**
     * Removes the element at the specified position in this list and returns it.
     * Shifts any subsequent elements to the left (subtracts one from their indices).
     *
     * @param index the index of the element to be removed. Must be between 0 and size()-1.
     * @return the element that was removed from the list
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size())
     */
    public byte delete(final int index) {
        rangeCheck(index);

        final byte oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     * Removes elements at the specified positions from this list.
     * The indices array specifies which elements to remove. Duplicate indices are ignored.
     * The implementation handles the removal efficiently by processing indices in sorted order.
     *
     * <p>This method is useful for batch removal operations when you have multiple
     * indices to remove. It's more efficient than removing elements one by one.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of((byte)10, (byte)20, (byte)30, (byte)40, (byte)50);
     * list.deleteAllByIndices(1, 3);  // Remove elements at positions 1 and 3
     * // list now contains: [10, 30, 50]
     * }</pre>
     *
     * @param indices the indices of elements to remove. Can be null or empty.
     *                Invalid indices (negative or >= size) are ignored.
     *
     */
    @Override
    public void deleteAllByIndices(final int... indices) {
        if (N.isEmpty(indices)) {
            return;
        }

        final byte[] tmp = N.deleteAllByIndices(elementData, indices);

        N.copy(tmp, 0, elementData, 0, tmp.length);

        if (size > tmp.length) {
            N.fill(elementData, tmp.length, size, (byte) 0);
        }

        size = size - (elementData.length - tmp.length);
    }

    /**
     * Removes a range of elements from this list.
     * The range to be removed extends from fromIndex (inclusive) to toIndex (exclusive).
     * Shifts any subsequent elements to the left (reduces their indices by toIndex-fromIndex).
     *
     * @param fromIndex the index of the first element to be removed (inclusive)
     * @param toIndex the index after the last element to be removed (exclusive)
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds,
     *         or if fromIndex > toIndex
     */
    @Override
    public void deleteRange(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (fromIndex == toIndex) {
            return;
        }

        final int size = size();//NOSONAR
        final int newSize = size - (toIndex - fromIndex);

        if (toIndex < size) {
            System.arraycopy(elementData, toIndex, elementData, fromIndex, size - toIndex);
        }

        N.fill(elementData, newSize, size, (byte) 0);

        this.size = newSize;
    }

    /**
     * Moves a range of elements within this list to a new position.
     * The elements from fromIndex (inclusive) to toIndex (exclusive) are moved
     * so that the element originally at fromIndex will be at newPositionAfterMove.
     * Other elements are shifted as necessary to accommodate the move.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5);
     * // size = 5
     *
     * // Move elements at indices 1,2 to start at index 2
     * list.moveRange(1, 3, 2);
     * // Result: [1, 4, 2, 3, 5]
     *
     * // Move range to end
     * list = ByteList.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5);
     * int rangeLen = 3 - 1; // = 2
     * list.moveRange(1, 3, list.size() - rangeLen); // newPos = 5 - 2 = 3
     * // Result: [1, 4, 5, 2, 3]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to be moved
     * @param toIndex the ending index (exclusive) of the range to be moved
     * @param newPositionAfterMove — the zero-based index where the first element of the range will be placed after the move; 
     *      must be between 0 and size() - lengthOfRange, inclusive.
     * @throws IndexOutOfBoundsException if any index is out of bounds or if
     *         newPositionAfterMove would cause elements to be moved outside the list
     */
    @Override
    public void moveRange(final int fromIndex, final int toIndex, final int newPositionAfterMove) {
        N.moveRange(elementData, fromIndex, toIndex, newPositionAfterMove);
    }

    /**
     * Replaces a range of elements in this list with elements from the specified ByteList.
     * The range to be replaced extends from fromIndex (inclusive) to toIndex (exclusive).
     * The size of the list may change if the replacement has a different number of elements
     * than the range being replaced.
     *
     * @param fromIndex the index of the first element to be replaced (inclusive)
     * @param toIndex the index after the last element to be replaced (exclusive)
     * @param replacement the ByteList containing elements to insert. Can be null or empty,
     *                    in which case the range is simply removed.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds,
     *         or if fromIndex > toIndex
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final ByteList replacement) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (N.isEmpty(replacement)) {
            deleteRange(fromIndex, toIndex);
            return;
        }

        final int size = this.size;//NOSONAR
        final int newSize = size - (toIndex - fromIndex) + replacement.size();

        if (elementData.length < newSize) {
            elementData = N.copyOf(elementData, newSize);
        }

        if (toIndex - fromIndex != replacement.size() && toIndex != size) {
            N.copy(elementData, toIndex, elementData, fromIndex + replacement.size(), size - toIndex);
        }

        N.copy(replacement.elementData, 0, elementData, fromIndex, replacement.size());

        if (newSize < size) {
            N.fill(elementData, newSize, size, (byte) 0);
        }

        this.size = newSize;
    }

    /**
     * Replaces a range of elements in this list with elements from the specified array.
     * The range to be replaced extends from fromIndex (inclusive) to toIndex (exclusive).
     * The size of the list may change if the replacement array has a different number of elements
     * than the range being replaced.
     *
     * @param fromIndex the index of the first element to be replaced (inclusive)
     * @param toIndex the index after the last element to be replaced (exclusive)
     * @param replacement the array containing elements to insert. Can be null or empty,
     *                    in which case the range is simply removed.
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds,
     *         or if fromIndex > toIndex
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final byte[] replacement) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (N.isEmpty(replacement)) {
            deleteRange(fromIndex, toIndex);
            return;
        }

        final int size = this.size;//NOSONAR
        final int newSize = size - (toIndex - fromIndex) + replacement.length;

        if (elementData.length < newSize) {
            elementData = N.copyOf(elementData, newSize);
        }

        if (toIndex - fromIndex != replacement.length && toIndex != size) {
            N.copy(elementData, toIndex, elementData, fromIndex + replacement.length, size - toIndex);
        }

        N.copy(replacement, 0, elementData, fromIndex, replacement.length);

        if (newSize < size) {
            N.fill(elementData, newSize, size, (byte) 0);
        }

        this.size = newSize;
    }

    /**
     * Replaces all occurrences of the specified value with a new value throughout the list.
     * The search and replacement is performed on all elements from index 0 to size-1.
     *
     * @param oldVal the value to be replaced
     * @param newVal the value to replace oldVal
     * @return the number of elements that were replaced
     */
    public int replaceAll(final byte oldVal, final byte newVal) {
        if (size() == 0) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = size(); i < len; i++) {
            if (elementData[i] == oldVal) {
                elementData[i] = newVal;

                result++;
            }
        }

        return result;
    }

    /**
     * Replaces each element of this list with the result of applying the specified operator to that element.
     * The operator is applied to each element in order from index 0 to size-1.
     *
     * <p>This method provides a functional way to transform all elements in place, useful for
     * operations like scaling, negation, or mathematical transformations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of((byte)1, (byte)2, (byte)3);
     * list.replaceAll(b -> (byte)(b * 2));  // Double each value
     * // list now contains: [2, 4, 6]
     * }</pre>
     *
     * @param operator the operator to apply to each element. Must not be null.
     *
     */
    public void replaceAll(final ByteUnaryOperator operator) {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsByte(elementData[i]);
        }
    }

    /**
     * Replaces all elements that satisfy the given predicate with the specified new value.
     * Elements are tested with the predicate in order from index 0 to size-1,
     * and those for which the predicate returns {@code true} are replaced with {@code newValue}.
     *
     * <p>This method provides a convenient way to conditionally replace values without
     * manually iterating through the list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of((byte)1, (byte)-2, (byte)3, (byte)-4, (byte)5);
     * list.replaceIf(b -> b < 0, (byte)0);  // Replace negative values with 0
     * // list now contains: [1, 0, 3, 0, 5]
     * }</pre>
     *
     * @param predicate the predicate to test each element. Must not be null.
     * @param newValue the value to replace matching elements with
     * @return {@code true} if at least one element was replaced; {@code false} if no elements matched
     *
     */
    public boolean replaceIf(final BytePredicate predicate, final byte newValue) {
        boolean result = false;

        for (int i = 0, len = size(); i < len; i++) {
            if (predicate.test(elementData[i])) {
                elementData[i] = newValue;

                result = true;
            }
        }

        return result;
    }

    /**
     * Replaces all elements in this list with the specified value.
     * After this call, every element from index 0 to size-1 will have the same value.
     *
     * @param val the value to fill the list with
     */
    public void fill(final byte val) {
        fill(0, size(), val);
    }

    /**
     * Replaces elements in the specified range of this list with the specified value.
     * The range extends from fromIndex (inclusive) to toIndex (exclusive).
     *
     * @param fromIndex the index of the first element (inclusive) to be filled
     * @param toIndex the index after the last element (exclusive) to be filled
     * @param val the value to fill the range with
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds,
     *         or if fromIndex > toIndex
     */
    public void fill(final int fromIndex, final int toIndex, final byte val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     * More formally, returns {@code true} if and only if this list contains
     * at least one element {@code e} such that {@code e == valueToFind}.
     *
     * <p>This method performs a linear search through the list.
     *
     * @param valueToFind the element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element, {@code false} otherwise
     */
    public boolean contains(final byte valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     * Returns {@code true} if this list contains any element from the specified ByteList.
     * This method returns {@code true} if there is at least one element that appears
     * in both lists.
     *
     * @param c the ByteList to check for common elements. Can be null.
     * @return {@code true} if this list contains any element from the specified list,
     *         {@code false} if either list is empty or null
     */
    @Override
    public boolean containsAny(final ByteList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    /**
     * Returns {@code true} if this list contains any element from the specified array.
     * This method returns {@code true} if there is at least one element that appears
     * in both this list and the array.
     *
     * @param a the array to check for common elements. Can be null.
     * @return {@code true} if this list contains any element from the specified array,
     *         {@code false} if either this list or the array is empty or null
     */
    @Override
    public boolean containsAny(final byte[] a) {
        if (isEmpty() || N.isEmpty(a)) {
            return false;
        }

        return !disjoint(a);
    }

    /**
     * Returns {@code true} if this list contains all elements from the specified ByteList.
     * This method returns {@code true} if every element in the specified list
     * is also present in this list. Duplicate elements are considered independently.
     *
     * @param c the ByteList to check for containment. Can be null.
     * @return {@code true} if this list contains all elements from the specified list,
     *         {@code true} if the specified list is null or empty,
     *         {@code false} otherwise
     */
    @Override
    public boolean containsAll(final ByteList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        if (needToSet(size(), c.size())) {
            final Set<Byte> set = this.toSet();

            for (int i = 0, len = c.size(); i < len; i++) {
                if (!set.contains(c.elementData[i])) {
                    return false;
                }
            }
        } else {
            for (int i = 0, len = c.size(); i < len; i++) {
                if (!contains(c.elementData[i])) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns {@code true} if this list contains all elements from the specified array.
     * This method returns {@code true} if every element in the specified array
     * is also present in this list. Duplicate elements are considered independently.
     *
     * @param a the array to check for containment. Can be null.
     * @return {@code true} if this list contains all elements from the specified array,
     *         {@code true} if the specified array is null or empty,
     *         {@code false} otherwise
     */
    @Override
    public boolean containsAll(final byte[] a) {
        if (N.isEmpty(a)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        return containsAll(of(a));
    }

    /**
     * Returns {@code true} if this list and the specified ByteList have no elements in common.
     * Two lists are disjoint if they share no common elements.
     *
     * @param c the ByteList to check for common elements. Can be null.
     * @return {@code true} if the two lists have no elements in common,
     *         {@code true} if either list is null or empty,
     *         {@code false} otherwise
     */
    @Override
    public boolean disjoint(final ByteList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        if (needToSet(size(), c.size())) {
            final Set<Byte> set = this.toSet();

            for (int i = 0, len = c.size(); i < len; i++) {
                if (set.contains(c.elementData[i])) {
                    return false;
                }
            }
        } else {
            for (int i = 0, len = c.size(); i < len; i++) {
                if (contains(c.elementData[i])) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns {@code true} if this list and the specified array have no elements in common.
     * This list and the array are disjoint if they share no common elements.
     *
     * @param b the array to check for common elements. Can be null.
     * @return {@code true} if this list and the array have no elements in common,
     *         {@code true} if either this list or the array is null or empty,
     *         {@code false} otherwise
     */
    @Override
    public boolean disjoint(final byte[] b) {
        if (isEmpty() || N.isEmpty(b)) {
            return true;
        }

        return disjoint(of(b));
    }

    /**
     * Returns a new ByteList containing the intersection of this list and the specified list.
     * The intersection contains elements that are present in both lists. For elements that
     * appear multiple times, the intersection contains the minimum number of occurrences
     * from either list. The order of elements from this list is preserved.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list1 = ByteList.of((byte)1, (byte)1, (byte)2, (byte)3);
     * ByteList list2 = ByteList.of((byte)1, (byte)2, (byte)2, (byte)4);
     * ByteList result = list1.intersection(list2); // result: [(byte)1, (byte)2]
     * }</pre>
     *
     * @param b the list to intersect with this list. Can be null.
     * @return a new ByteList containing the intersection of the two lists.
     *         Returns an empty list if either list is null or empty.
     * @see #difference(ByteList)
     * @see #symmetricDifference(ByteList)
     */
    @Override
    public ByteList intersection(final ByteList b) {
        if (N.isEmpty(b)) {
            return new ByteList();
        }

        final Multiset<Byte> bOccurrences = b.toMultiset();

        final ByteList c = new ByteList(N.min(9, size(), b.size()));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Returns a new ByteList containing the intersection of this list and the specified array.
     * The intersection contains elements that are present in both this list and the array.
     * For elements that appear multiple times, the intersection contains the minimum number
     * of occurrences from either source. The order of elements from this list is preserved.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of((byte)1, (byte)1, (byte)2, (byte)3);
     * byte[] array = {(byte)1, (byte)2, (byte)2, (byte)4};
     * ByteList result = list.intersection(array); // result: [(byte)1, (byte)2]
     * }</pre>
     *
     * @param b the array to intersect with this list. Can be null.
     * @return a new ByteList containing the intersection of this list and the array.
     *         Returns an empty list if the array is null or empty.
     * @see #difference(byte[])
     * @see #symmetricDifference(byte[])
     */
    @Override
    public ByteList intersection(final byte[] b) {
        if (N.isEmpty(b)) {
            return new ByteList();
        }

        return intersection(of(b));
    }

    /**
     * Returns a new ByteList containing elements that are in this list but not in the specified list.
     * For elements that appear multiple times, the difference operation considers the count of occurrences.
     * If an element appears m times in this list and n times in the specified list, the result will
     * contain max(0, m-n) occurrences of that element. The order of elements is preserved.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list1 = ByteList.of((byte)1, (byte)1, (byte)2, (byte)3);
     * ByteList list2 = ByteList.of((byte)1, (byte)4);
     * ByteList result = list1.difference(list2); // result: [(byte)1, (byte)2, (byte)3]
     * }</pre>
     *
     * @param b the list containing elements to be excluded. Can be null.
     * @return a new ByteList containing elements in this list but not in the specified list.
     *         Returns a copy of this list if the specified list is null or empty.
     * @see #intersection(ByteList)
     * @see #symmetricDifference(ByteList)
     */
    @Override
    public ByteList difference(final ByteList b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Byte> bOccurrences = b.toMultiset();

        final ByteList c = new ByteList(N.min(size(), N.max(9, size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (!bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Returns a new ByteList containing elements that are in this list but not in the specified array.
     * For elements that appear multiple times, the difference operation considers the count of occurrences.
     * If an element appears m times in this list and n times in the array, the result will
     * contain max(0, m-n) occurrences of that element. The order of elements is preserved.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of((byte)1, (byte)1, (byte)2, (byte)3);
     * byte[] array = {(byte)1, (byte)4};
     * ByteList result = list.difference(array); // result: [(byte)1, (byte)2, (byte)3]
     * }</pre>
     *
     * @param b the array containing elements to be excluded. Can be null.
     * @return a new ByteList containing elements in this list but not in the specified array.
     *         Returns a copy of this list if the array is null or empty.
     * @see #intersection(byte[])
     * @see #symmetricDifference(byte[])
     */
    @Override
    public ByteList difference(final byte[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(b));
    }

    /**
     * Returns a new ByteList containing the symmetric difference between this list and the specified list.
     * The symmetric difference contains elements that are in either list but not in both.
     * For elements that appear multiple times, the result contains the absolute difference in counts.
     * The order is preserved with elements from this list appearing first, followed by elements
     * unique to the specified list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list1 = ByteList.of((byte)1, (byte)1, (byte)2, (byte)3);
     * ByteList list2 = ByteList.of((byte)2, (byte)3, (byte)3, (byte)4);
     * ByteList result = list1.symmetricDifference(list2); 
     * // result: [(byte)1, (byte)1, (byte)3, (byte)4]
     * }</pre>
     *
     * @param b the list to compare with this list. Can be null.
     * @return a new ByteList containing elements that are in either list but not in both.
     *         Returns a copy of this list if the specified list is null or empty.
     * @see #difference(ByteList)
     * @see #intersection(ByteList)
     */
    @Override
    public ByteList symmetricDifference(final ByteList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (isEmpty()) {
            return b.copy();
        }

        final Multiset<Byte> bOccurrences = b.toMultiset();
        final ByteList c = new ByteList(N.max(9, Math.abs(size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (!bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        for (int i = 0, len = b.size(); i < len; i++) {
            if (bOccurrences.remove(b.elementData[i])) {
                c.add(b.elementData[i]);
            }

            if (bOccurrences.isEmpty()) {
                break;
            }
        }

        return c;
    }

    /**
     * Returns a new ByteList containing the symmetric difference between this list and the specified array.
     * The symmetric difference contains elements that are in either this list or the array but not in both.
     * For elements that appear multiple times, the result contains the absolute difference in counts.
     * The order is preserved with elements from this list appearing first, followed by elements
     * unique to the array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of((byte)1, (byte)1, (byte)2, (byte)3);
     * byte[] array = {(byte)2, (byte)3, (byte)3, (byte)4};
     * ByteList result = list.symmetricDifference(array); 
     * // result: [(byte)1, (byte)1, (byte)3, (byte)4]
     * }</pre>
     *
     * @param b the array to compare with this list. Can be null.
     * @return a new ByteList containing elements that are in either this list or the array but not in both.
     *         Returns a copy of this list if the array is null or empty,
     *         or a new list containing the array elements if this list is empty.
     * @see #difference(byte[])
     * @see #intersection(byte[])
     */
    @Override
    public ByteList symmetricDifference(final byte[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        } else if (isEmpty()) {
            return of(N.copyOfRange(b, 0, b.length));
        }

        return symmetricDifference(of(b));
    }

    /**
     * Returns the number of times the specified value appears in this list.
     * This method counts all occurrences of the value throughout the entire list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of((byte)1, (byte)2, (byte)1, (byte)3, (byte)1);
     * int count = list.occurrencesOf((byte)1);  // Returns 3
     * }</pre>
     *
     * @param valueToFind the value to count occurrences of
     * @return the number of times the specified value appears in this list; 0 if the value is not found
     *
     */
    public int occurrencesOf(final byte valueToFind) {
        if (size == 0) {
            return 0;
        }

        int occurrences = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] == valueToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     * Returns the index of the first occurrence of the specified value in this list.
     * Searches the entire list from the beginning.
     *
     * @param valueToFind the value to search for
     * @return the index of the first occurrence of the specified value,
     *         or -1 if the value is not found
     */
    public int indexOf(final byte valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified value in this list,
     * starting the search at the specified index. The search proceeds forward from
     * fromIndex to the end of the list.
     *
     * @param valueToFind the value to search for
     * @param fromIndex the index to start searching from (inclusive).
     *                  If negative, it is treated as 0.
     * @return the index of the first occurrence of the specified value at or after fromIndex,
     *         or -1 if the value is not found
     */
    public int indexOf(final byte valueToFind, final int fromIndex) {
        if (fromIndex >= size) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.max(fromIndex, 0); i < size; i++) {
            if (elementData[i] == valueToFind) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the index of the last occurrence of the specified value in this list.
     * Searches the entire list from the end backwards.
     *
     * @param valueToFind the value to search for
     * @return the index of the last occurrence of the specified value,
     *         or -1 if the value is not found
     */
    public int lastIndexOf(final byte valueToFind) {
        return lastIndexOf(valueToFind, size - 1);
    }

    /**
     * Returns the index of the last occurrence of the specified value in this list,
     * searching backwards from the specified index. The search includes the element
     * at startIndexFromBack if it is within bounds.
     *
     * @param valueToFind the value to search for
     * @param startIndexFromBack the index to start searching backwards from (inclusive).
     *                           If greater than or equal to size, it is treated as size-1.
     * @return the index of the last occurrence of the specified value at or before
     *         startIndexFromBack, or -1 if the value is not found
     */
    public int lastIndexOf(final byte valueToFind, final int startIndexFromBack) {
        if (startIndexFromBack < 0 || size == 0) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, size - 1); i >= 0; i--) {
            if (elementData[i] == valueToFind) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Returns the minimum value in this list wrapped in an OptionalByte.
     * If the list is empty, returns an empty OptionalByte.
     *
     * @return an OptionalByte containing the minimum value, or an empty OptionalByte if the list is empty
     */
    public OptionalByte min() {
        return size() == 0 ? OptionalByte.empty() : OptionalByte.of(N.min(elementData, 0, size));
    }

    /**
     * Returns the minimum value in the specified range of this list wrapped in an OptionalByte.
     * The range extends from fromIndex (inclusive) to toIndex (exclusive).
     * If the range is empty, returns an empty OptionalByte.
     *
     * @param fromIndex the index of the first element (inclusive) to consider
     * @param toIndex the index after the last element (exclusive) to consider
     * @return an OptionalByte containing the minimum value in the range,
     *         or an empty OptionalByte if the range is empty
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of bounds,
     *         or if fromIndex > toIndex
     */
    public OptionalByte min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalByte.empty() : OptionalByte.of(N.min(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the maximum value in this list wrapped in an OptionalByte.
     * If the list is empty, returns an empty OptionalByte.
     *
     * @return an OptionalByte containing the maximum value, or an empty OptionalByte if the list is empty
     */
    public OptionalByte max() {
        return size() == 0 ? OptionalByte.empty() : OptionalByte.of(N.max(elementData, 0, size));
    }

    /**
     * Finds and returns the maximum byte value within the specified range of this list.
     * 
     * <p>This method scans through the elements from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
     * and returns the largest byte value found. If the range is empty (fromIndex == toIndex), an empty
     * OptionalByte is returned.</p>
     * 
     * <p>The comparison is performed using standard byte comparison, where bytes are treated as signed
     * values ranging from -128 to 127.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to search
     * @param toIndex the ending index (exclusive) of the range to search
     * @return an OptionalByte containing the maximum value if the range is non-empty, or an empty OptionalByte if the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    public OptionalByte max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalByte.empty() : OptionalByte.of(N.max(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the median value of all elements in this list.
     * 
     * <p>The median is the middle value when the elements are sorted in ascending order. For lists with
     * an odd number of elements, this is the exact middle element. For lists with an even number of
     * elements, this method returns the lower of the two middle elements (not the average).</p>
     *
     * @return an OptionalByte containing the median value if the list is non-empty, or an empty OptionalByte if the list is empty
     */
    public OptionalByte median() {
        return size() == 0 ? OptionalByte.empty() : OptionalByte.of(N.median(elementData, 0, size));
    }

    /**
     * Returns the median value of elements within the specified range of this list.
     * 
     * <p>The median is computed for elements from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     * For ranges with an odd number of elements, this returns the exact middle element when sorted.
     * For ranges with an even number of elements, this returns the lower of the two middle elements.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to calculate median for
     * @param toIndex the ending index (exclusive) of the range to calculate median for
     * @return an OptionalByte containing the median value if the range is non-empty, or an empty OptionalByte if the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    public OptionalByte median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalByte.empty() : OptionalByte.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     * Performs the given action for each element in this list in sequential order.
     * 
     * <p>This method iterates through all elements from index 0 to size-1, applying the specified
     * ByteConsumer action to each element. The action is performed in the order of iteration.</p>
     * 
     * <p>This is equivalent to:</>
     * <pre>{@code
     * for (int i = 0; i < size(); i++) {
     *     action.accept(get(i));
     * }
     * }</pre>
     *
     * @param action the action to be performed for each element
     */
    public void forEach(final ByteConsumer action) {
        forEach(0, size, action);
    }

    /**
     * Performs the given action for each element within the specified range of this list.
     * 
     * <p>This method supports both forward and backward iteration based on the relative values of
     * fromIndex and toIndex:</p>
     * <ul>
     *   <li>If {@code fromIndex <= toIndex}: iterates forward from fromIndex (inclusive) to toIndex (exclusive)</li>
     *   <li>If {@code fromIndex > toIndex}: iterates backward from fromIndex (inclusive) to toIndex (exclusive)</li>
     *   <li>If {@code toIndex == -1}: treated as backward iteration from fromIndex to the beginning of the list</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * list.forEach(0, 5, action);    // Forward: processes indices 0,1,2,3,4
     * list.forEach(5, 0, action);    // Backward: processes indices 5,4,3,2,1
     * list.forEach(5, -1, action);   // Backward: processes indices 5,4,3,2,1,0
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive), or -1 for backward iteration to the start
     * @param action the action to be performed for each element
     * @throws IndexOutOfBoundsException if the indices are out of range
     */
    public void forEach(final int fromIndex, final int toIndex, final ByteConsumer action) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex), size);

        if (size > 0) {
            if (fromIndex <= toIndex) {
                for (int i = fromIndex; i < toIndex; i++) {
                    action.accept(elementData[i]);
                }
            } else {
                for (int i = N.min(size - 1, fromIndex); i > toIndex; i--) {
                    action.accept(elementData[i]);
                }
            }
        }
    }

    /**
     * Returns the first element of this list wrapped in an OptionalByte.
     * 
     * <p>This method provides a null-safe way to access the first element of the list. If the list
     * is empty, it returns an empty OptionalByte rather than throwing an exception. This is useful
     * when you want to safely check for and retrieve the first element without explicit size checking.</p>
     * 
     * <p>This is equivalent to:</p>
     * <pre>{@code
     * size() == 0 ? OptionalByte.empty() : OptionalByte.of(get(0))
     * }</pre>
     *
     * @return an OptionalByte containing the first element if the list is non-empty, or an empty OptionalByte if the list is empty
     */
    public OptionalByte first() {
        return size() == 0 ? OptionalByte.empty() : OptionalByte.of(elementData[0]);
    }

    /**
     * Returns the last element of this list wrapped in an OptionalByte.
     * 
     * <p>This method provides a null-safe way to access the last element of the list. If the list
     * is empty, it returns an empty OptionalByte rather than throwing an exception. This is useful
     * when you want to safely check for and retrieve the last element without explicit size checking.</p>
     * 
     * <p>This is equivalent to:</p>
     * <pre>{@code
     * size() == 0 ? OptionalByte.empty() : OptionalByte.of(get(size() - 1))
     * }</pre>
     *
     * @return an OptionalByte containing the last element if the list is non-empty, or an empty OptionalByte if the list is empty
     */
    public OptionalByte last() {
        return size() == 0 ? OptionalByte.empty() : OptionalByte.of(elementData[size() - 1]);
    }

    /**
     * Returns a new ByteList containing only the distinct elements from the specified range of this list.
     * 
     * <p>This method creates a new list containing each unique byte value from the specified range,
     * preserving the order of first occurrence. Duplicate values are removed, keeping only the first
     * occurrence of each distinct value.</p>
     * 
     * <p>For example, if the range contains [1, 2, 2, 3, 1, 4], the returned list will contain [1, 2, 3, 4].</p>
     * 
     * <p>The time complexity is O(n) for small ranges using a boolean array for tracking seen values,
     * where n is the size of the range. The original list is not modified.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to process
     * @param toIndex the ending index (exclusive) of the range to process
     * @return a new ByteList containing only distinct elements from the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    @Override
    public ByteList distinct(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            return of(N.distinct(elementData, fromIndex, toIndex));
        } else {
            return of(N.copyOfRange(elementData, fromIndex, toIndex));
        }
    }

    /**
     * Checks whether this list contains any duplicate elements.
     * 
     * <p>This method scans through all elements in the list and returns {@code true} if any element appears
     * more than once. It uses an efficient algorithm that typically completes in O(n) time for byte
     * values by using a boolean array to track seen values.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list1 = ByteList.of(1, 2, 3, 4);      // hasDuplicates() returns false
     * ByteList list2 = ByteList.of(1, 2, 2, 3);      // hasDuplicates() returns true
     * ByteList list3 = ByteList.of();                // hasDuplicates() returns false
     * }</pre>
     *
     * @return {@code true} if this list contains at least one duplicate element, {@code false} otherwise
     */
    @Override
    public boolean hasDuplicates() {
        return N.hasDuplicates(elementData, 0, size, false);
    }

    /**
     * Checks whether the elements in this list are sorted in ascending order.
     * 
     * <p>This method verifies if each element in the list is less than or equal to the next element,
     * treating bytes as signed values (-128 to 127). An empty list or a list with a single element
     * is considered sorted.</p>
     * 
     * <p>The check is performed in O(n) time by comparing adjacent elements. The method returns as
     * soon as it finds an element that is greater than its successor.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList.of(-5, 0, 3, 10).isSorted()     // returns true
     * ByteList.of(3, 1, 4, 2).isSorted()       // returns false
     * ByteList.of(1, 1, 2, 2).isSorted()       // returns {@code true} (duplicates allowed)
     * }</pre>
     *
     * @return {@code true} if the list is sorted in ascending order, {@code false} otherwise
     */
    @Override
    public boolean isSorted() {
        return N.isSorted(elementData, 0, size);
    }

    /**
     * Sorts all elements in this list in ascending order.
     * 
     * <p>This method modifies the list in-place, rearranging elements so that they are in ascending
     * order. Bytes are compared as signed values, so the order will be from -128 to 127. The sort
     * is stable, meaning that equal elements retain their relative order.</p>
     * 
     * <p>The sorting algorithm used is optimized for primitive arrays and typically performs in
     * O(n log n) time. For small lists or lists that are already nearly sorted, the performance
     * may be better.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of(3, -1, 4, 1, 5);
     * list.sort();  // list now contains [-1, 1, 3, 4, 5]
     * }</pre>
     */
    @Override
    public void sort() {
        if (size > 1) {
            N.sort(elementData, 0, size);
        }
    }

    /**
     * Sorts all elements in this list in ascending order using a parallel sorting algorithm.
     * 
     * <p>This method is similar to {@link #sort()} but uses a parallel sorting algorithm that can
     * take advantage of multiple processor cores for better performance on large lists. The sort
     * is performed in-place and produces the same result as the sequential sort.</p>
     * 
     * <p>Parallel sorting is typically beneficial for lists with thousands of elements or more.
     * For smaller lists, the overhead of parallelization may make this method slower than the
     * sequential {@link #sort()} method.</p>
     * 
     * <p>The sort is stable and compares bytes as signed values (-128 to 127).</p>
     */
    public void parallelSort() {
        if (size > 1) {
            N.parallelSort(elementData, 0, size);
        }
    }

    /**
     * Sorts all elements in this list in descending order.
     * 
     * <p>This method first sorts the list in ascending order and then reverses it to achieve
     * descending order. The result is that elements are arranged from highest to lowest value,
     * with bytes compared as signed values (127 down to -128).</p>
     * 
     * <p>This is equivalent to calling {@link #sort()} followed by {@link #reverse()}, but may
     * be slightly more efficient as it's implemented as a single operation.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of(3, -1, 4, 1, 5);
     * list.reverseSort();  // list now contains [5, 4, 3, 1, -1]
     * }</pre>
     */
    @Override
    public void reverseSort() {
        if (size > 1) {
            sort();
            reverse();
        }
    }

    /**
     * Searches for the specified byte value in this sorted list using binary search algorithm.
     * 
     * <p>This method requires the list to be sorted in ascending order before calling. If the list
     * is not sorted, the results are undefined. The binary search algorithm provides O(log n)
     * performance, making it much faster than linear search for large sorted lists.</p>
     * 
     * <p>The method returns the index of the search key if it is contained in the list. If the key
     * is not found, it returns {@code (-(insertion point) - 1)}, where insertion point is the index
     * at which the key would be inserted to maintain sorted order.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of(1, 3, 5, 7, 9);
     * list.binarySearch((byte)5);   // returns 2
     * list.binarySearch((byte)6);   // returns -4 (insertion point would be 3)
     * }</pre>
     *
     * @param valueToFind the byte value to search for
     * @return the index of the search key if found; otherwise, {@code (-(insertion point) - 1)}
     */
    public int binarySearch(final byte valueToFind) {
        return N.binarySearch(elementData, 0, size(), valueToFind);
    }

    /**
     * Searches for the specified byte value within a range of this sorted list using binary search.
     * 
     * <p>This method performs a binary search on the specified range of the list, from {@code fromIndex}
     * (inclusive) to {@code toIndex} (exclusive). The range must be sorted in ascending order before
     * calling this method, or the results are undefined.</p>
     * 
     * <p>Like the full-list binary search, this returns the index if found, or {@code (-(insertion point) - 1)}
     * if not found. The insertion point is relative to the entire list, not just the searched range.</p>
     * 
     * <p>This method is useful when you know the value you're searching for is likely to be in a
     * specific portion of a large sorted list.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to search
     * @param toIndex the ending index (exclusive) of the range to search
     * @param valueToFind the byte value to search for
     * @return the index of the search key if found; otherwise, {@code (-(insertion point) - 1)}
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    public int binarySearch(final int fromIndex, final int toIndex, final byte valueToFind) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return N.binarySearch(elementData, fromIndex, toIndex, valueToFind);
    }

    /**
     * Reverses the order of all elements in this list.
     * 
     * <p>This method modifies the list in-place, swapping elements from both ends moving toward
     * the center. After this operation, the first element becomes the last, the second becomes
     * the second-to-last, and so on.</p>
     * 
     * <p>The operation is performed in O(n/2) time, where n is the size of the list.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of(1, 2, 3, 4, 5);
     * list.reverse();  // list now contains [5, 4, 3, 2, 1]
     * }</pre>
     */
    @Override
    public void reverse() {
        if (size > 1) {
            N.reverse(elementData, 0, size);
        }
    }

    /**
     * Reverses the order of elements within the specified range of this list.
     * 
     * <p>This method modifies the list in-place, reversing only the elements from {@code fromIndex}
     * (inclusive) to {@code toIndex} (exclusive). Elements outside this range remain unchanged.
     * The reversal is done by swapping elements from both ends of the range moving toward the center.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of(1, 2, 3, 4, 5, 6);
     * list.reverse(1, 5);  // list now contains [1, 5, 4, 3, 2, 6]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to reverse
     * @param toIndex the ending index (exclusive) of the range to reverse
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    @Override
    public void reverse(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            N.reverse(elementData, fromIndex, toIndex);
        }
    }

    /**
     * Rotates all elements in this list by the specified distance.
     * After calling rotate(distance), the element at index i will be moved to
     * index (i + distance) % size. 
     * 
     * <p>Positive values of distance rotate elements towards higher indices (right rotation),
     * while negative values rotate towards lower indices (left rotation).
     * The list is modified in place.</p>
     *
     * @param distance the distance to rotate the list. Positive values rotate right,
     *                 negative values rotate left
     * @see N#rotate(int[], int)
     */
    @Override
    public void rotate(final int distance) {
        if (size > 1) {
            N.rotate(elementData, 0, size, distance);
        }
    }

    /**
     * Randomly shuffles all elements in this list.
     * 
     * <p>This method rearranges the elements in random order using the default random number generator.
     * Each possible permutation of the list has equal probability of being produced. The shuffle is
     * performed in-place using the Fisher-Yates algorithm, which runs in O(n) time.</p>
     * 
     * <p>This method uses a default source of randomness that is suitable for most applications
     * but not for cryptographic purposes.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of(1, 2, 3, 4, 5);
     * list.shuffle();  // list might now contain [3, 1, 5, 2, 4]
     * }</pre>
     */
    @Override
    public void shuffle() {
        if (size() > 1) {
            N.shuffle(elementData, 0, size);
        }
    }

    /**
     * Randomly shuffles all elements in this list using the specified random number generator.
     * 
     * <p>This method rearranges the elements in random order using the provided Random object.
     * This allows you to control the randomness source, which is useful for:</p>
     * <ul>
     *   <li>Reproducible shuffles using a seeded Random</li>
     *   <li>Cryptographically strong randomness using SecureRandom</li>
     *   <li>Custom random number generators</li>
     * </ul>
     * 
     * <p>The shuffle is performed in-place using the Fisher-Yates algorithm in O(n) time.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of(1, 2, 3, 4, 5);
     * Random rnd = new Random(12345);  // Seeded for reproducibility
     * list.shuffle(rnd);  // Will always produce the same shuffle with this seed
     * }</pre>
     *
     * @param rnd the random number generator to use for shuffling
     */
    @Override
    public void shuffle(final Random rnd) {
        if (size() > 1) {
            N.shuffle(elementData, 0, size, rnd);
        }
    }

    /**
     * Swaps the elements at the specified positions in this list.
     * 
     * <p>This method exchanges the elements at indices {@code i} and {@code j}. If {@code i} and {@code j}
     * are equal, the list is unchanged. This operation is useful for custom sorting algorithms or
     * manual list reordering.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of(1, 2, 3, 4, 5);
     * list.swap(1, 3);  // list now contains [1, 4, 3, 2, 5]
     * }</pre>
     *
     * @param i the index of the first element to swap
     * @param j the index of the second element to swap
     * @throws IndexOutOfBoundsException if either index is out of range ({@code < 0 || >= size()})
     */
    @Override
    public void swap(final int i, final int j) {
        rangeCheck(i);
        rangeCheck(j);

        set(i, set(j, elementData[i]));
    }

    /**
     * Creates and returns a copy of this entire list.
     * 
     * <p>This method creates a new ByteList instance containing all elements from this list in the
     * same order. The returned list is independent of this list; changes to either list will not
     * affect the other. The element array is copied, so this is a "deep copy" with respect to the
     * primitive values.</p>
     * 
     * <p>This is equivalent to calling {@code copy(0, size())} but may be slightly more efficient.</p>
     *
     * @return a new ByteList containing all elements from this list
     */
    @Override
    public ByteList copy() {
        return new ByteList(N.copyOfRange(elementData, 0, size));
    }

    /**
     * Creates and returns a copy of the specified range of this list.
     * 
     * <p>This method creates a new ByteList containing elements from {@code fromIndex} (inclusive)
     * to {@code toIndex} (exclusive). The returned list is independent of this list; changes to
     * either list will not affect the other.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of(1, 2, 3, 4, 5);
     * ByteList subCopy = list.copy(1, 4);  // subCopy contains [2, 3, 4]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to copy
     * @param toIndex the ending index (exclusive) of the range to copy
     * @return a new ByteList containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    @Override
    public ByteList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new ByteList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     * Creates and returns a copy of elements from the specified range with the given step.
     * 
     * <p>This method creates a new ByteList by copying elements starting from {@code fromIndex},
     * moving by {@code step} positions each time, until reaching {@code toIndex}. This allows for
     * operations like selecting every nth element or reversing with a specific stride.</p>
     * 
     * <p>The step parameter determines the direction and stride:</p>
     * <ul>
     *   <li>Positive step: moves forward through the list</li>
     *   <li>Negative step: moves backward through the list</li>
     *   <li>Step magnitude > 1: skips elements</li>
     * </ul>
     * 
     * <p>Special handling for reverse iteration: if {@code fromIndex > toIndex}, the indices are
     * automatically adjusted, and if {@code toIndex == -1}, it's treated as reverse iteration to
     * the start of the list.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of(0, 1, 2, 3, 4, 5);
     * list.copy(0, 6, 2);   // returns [0, 2, 4] (every other element)
     * list.copy(5, -1, -2); // returns [5, 3, 1] (reverse, every other)
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive), or -1 for reverse iteration to start
     * @param step the step size (positive for forward, negative for backward)
     * @return a new ByteList containing the selected elements
     * @throws IndexOutOfBoundsException if the indices are out of range
     * @throws IllegalArgumentException if step is 0
     * @see N#copyOfRange(byte[], int, int, int)
     */
    @Override
    public ByteList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex));

        return new ByteList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Splits this list into multiple consecutive sublists of the specified size.
     * 
     * <p>This method divides the specified range of the list into chunks of the given size. All chunks
     * except possibly the last one will have exactly {@code chunkSize} elements. The last chunk may
     * be smaller if the range size is not evenly divisible by the chunk size.</p>
     * 
     * <p>Each returned ByteList is independent (a copy of the data), so modifications to the returned
     * lists do not affect this list or each other.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of(1, 2, 3, 4, 5, 6, 7);
     * List<ByteList> chunks = list.split(0, 7, 3);
     * // chunks contains: [[1, 2, 3], [4, 5, 6], [7]]
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to split
     * @param toIndex the ending index (exclusive) of the range to split
     * @param chunkSize the desired size of each chunk (must be positive)
     * @return a List of ByteList objects, each containing a chunk of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     * @throws IllegalArgumentException if {@code chunkSize <= 0}
     */
    @Override
    public List<ByteList> split(final int fromIndex, final int toIndex, final int chunkSize) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<byte[]> list = N.split(elementData, fromIndex, toIndex, chunkSize);
        @SuppressWarnings("rawtypes")
        final List<ByteList> result = (List) list;

        for (int i = 0, len = list.size(); i < len; i++) {
            result.set(i, of(list.get(i)));
        }

        return result;
    }

    /**
     * Trims the internal array to the current size of the list.
     * 
     * <p>This method reduces the capacity of the internal array to match the actual number of elements
     * in the list, potentially saving memory. This is useful after a large number of removals or when
     * you know the list size won't increase significantly.</p>
     * 
     * <p>The operation creates a new internal array of exactly the right size and copies existing
     * elements to it. This is an O(n) operation where n is the current size of the list.</p>
     * 
     * <p>Note: This method returns the list itself to allow method chaining.</p>
     *
     * @return this ByteList instance (for method chaining)
     */
    @Override
    public ByteList trimToSize() {
        if (elementData.length > size) {
            elementData = N.copyOfRange(elementData, 0, size);
        }

        return this;
    }

    /**
     * Removes all elements from this list. The list will be empty after this call returns.
     * The capacity of the list is not changed.
     */
    @Override
    public void clear() {
        if (size > 0) {
            N.fill(elementData, 0, size, (byte) 0);
        }

        size = 0;
    }

    /**
     * Returns {@code true} if this list contains no elements.
     *
     * @return {@code true} if this list contains no elements, {@code false} otherwise
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns a List containing boxed Byte objects for all elements in this list.
     * 
     * <p>This method creates a new ArrayList<Byte> and populates it with boxed versions of all
     * primitive byte values in this list. The order of elements is preserved. This is useful when
     * you need to interface with APIs that require object types rather than primitives.</p>
     * 
     * <p>Note: Boxing primitive values has memory and performance overhead. Each byte value will
     * be wrapped in a Byte object, which typically requires 16+ bytes of memory compared to 1 byte
     * for the primitive value.</p>
     *
     * @return a new List<Byte> containing boxed versions of all elements
     */
    @Override
    public List<Byte> boxed() {
        return boxed(0, size);
    }

    /**
     * Returns a List containing boxed Byte objects for elements in the specified range.
     * 
     * <p>This method creates a new ArrayList<Byte> containing boxed versions of the primitive byte
     * values from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive). The relative order
     * of elements is preserved.</p>
     * 
     * <p>This is useful when you need to:</p>
     * <ul>
     *   <li>Pass a subset of values to APIs requiring object types</li>
     *   <li>Create a mutable List<Byte> from a range of primitive values</li>
     *   <li>Interface with collections frameworks that don't support primitives</li>
     * </ul>
     *
     * @param fromIndex the starting index (inclusive) of the range to box
     * @param toIndex the ending index (exclusive) of the range to box
     * @return a new List<Byte> containing boxed versions of the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    @Override
    public List<Byte> boxed(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Byte> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    /**
     * Returns a new array containing all elements of this list in proper sequence.
     *
     * @return a new byte array containing all elements of this list
     */
    @Override
    public byte[] toArray() {
        return N.copyOfRange(elementData, 0, size);
    }

    /**
     * Converts this ByteList to an IntList.
     * 
     * <p>This method creates a new IntList where each byte value from this list is promoted to an
     * int value. The conversion preserves the sign of byte values, so negative bytes become negative
     * ints (e.g., byte -1 becomes int -1, not 255).</p>
     * 
     * <p>This conversion is useful when you need to:</p>
     * <ul>
     *   <li>Perform arithmetic operations that might overflow byte range</li>
     *   <li>Interface with APIs that work with int values</li>
     *   <li>Avoid repeated byte-to-int conversions in calculations</li>
     * </ul>
     * 
     * <p>Note: This creates a new list with a larger memory footprint (4 bytes per element instead
     * of 1 byte).</p>
     *
     * @return a new IntList containing all elements from this list converted to int values
     */
    public IntList toIntList() {
        final int[] a = new int[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i]; //NOSONAR
        }

        return IntList.of(a);
    }

    /**
     * Returns a Collection containing the elements from the specified range converted to their boxed type.
     * The type of Collection returned is determined by the provided supplier function.
     * The returned collection is independent of this list.
     *
     * @param <C> the type of Collection to create
     * @param fromIndex the starting index (inclusive) of elements to include
     * @param toIndex the ending index (exclusive) of elements to include
     * @param supplier a function that creates a new Collection instance given the required size
     * @return a new Collection containing boxed elements from the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    @Override
    public <C extends Collection<Byte>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final C c = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            c.add(elementData[i]);
        }

        return c;
    }

    /**
     * Returns a Multiset containing all elements from specified range converted to their boxed type.
     * The type of Multiset returned is determined by the provided supplier function.
     * A Multiset is a collection that allows duplicate elements and provides occurrence counting.
     *
     * @param fromIndex the starting index (inclusive) of elements to include
     * @param toIndex the ending index (exclusive) of elements to include
     * @param supplier a function that creates a new Multiset instance given the required size
     * @return a new Multiset containing elements from the specified range with their counts
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    @Override
    public Multiset<Byte> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Byte>> supplier) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Byte> multiset = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            multiset.add(elementData[i]);
        }

        return multiset;
    }

    /**
     * Returns an iterator over all elements in this list.
     * 
     * <p>This method returns a specialized ByteIterator that efficiently iterates over the primitive
     * byte values without boxing. The iterator supports standard operations like hasNext() and nextByte(),
     * providing better performance than a generic Iterator<Byte>.</p>
     * 
     * <p>The returned iterator:</p>
     * <ul>
     *   <li>Iterates elements in index order (0 to size-1)</li>
     *   <li>Returns an empty iterator if the list is empty</li>
     *   <li>Does not support element removal</li>
     *   <li>Fails fast if the list is structurally modified during iteration</li>
     * </ul>
     *
     * @return a ByteIterator over all elements in this list
     */
    @Override
    public ByteIterator iterator() {
        if (isEmpty()) {
            return ByteIterator.EMPTY;
        }

        return ByteIterator.of(elementData, 0, size);
    }

    /**
     * Returns a ByteStream with this list as its source.
     * 
     * <p>This method creates a stream that allows functional-style operations on the byte values
     * in this list. The stream is sequential and ordered, processing elements in their index order.
     * Stream operations do not modify the original list.</p>
     * 
     * <p>ByteStream provides specialized primitive operations that avoid boxing overhead, making it
     * more efficient than Stream<Byte> for primitive byte values.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of(1, 2, 3, 4, 5);
     * int sum = list.stream()
     *     .filter(b -> b > 2)
     *     .map(b -> b * 2)
     *     .sum();  // Sum of (3*2 + 4*2 + 5*2) = 24
     * }</pre>
     *
     * @return a ByteStream over all elements in this list
     */
    public ByteStream stream() {
        return ByteStream.of(elementData, 0, size());
    }

    /**
     * Returns a ByteStream for the specified range of this list.
     * 
     * <p>This method creates a stream over elements from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive). The stream processes elements in their index order and does
     * not modify the original list. This is useful for applying stream operations to a subset
     * of the list without creating a temporary sublist.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteList.of(1, 2, 3, 4, 5, 6);
     * long count = list.stream(2, 5)
     *     .filter(b -> b % 2 == 0)
     *     .count();  // Counts even numbers in elements [3, 4, 5], result is 1
     * }</pre>
     *
     * @param fromIndex the starting index (inclusive) for the stream
     * @param toIndex the ending index (exclusive) for the stream
     * @return a ByteStream over the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    public ByteStream stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return ByteStream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in this list.
     * 
     * <p>This method provides direct access to the first element without using Optional. Unlike
     * {@link #first()}, this method throws an exception if the list is empty, making it suitable
     * when you know the list is non-empty or want to fail fast.</p>
     *
     * @return the first byte value in the list
     * @throws NoSuchElementException if the list is empty
     */
    public byte getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in this list.
     * 
     * <p>This method provides direct access to the last element without using Optional. Unlike
     * {@link #last()}, this method throws an exception if the list is empty, making it suitable
     * when you know the list is non-empty or want to fail fast.</p>
     *
     * @return the last byte value in the list
     * @throws NoSuchElementException if the list is empty
     */
    public byte getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list.
     * 
     * <p>This method adds the element at index 0, shifting all existing elements one position to
     * the right. This is equivalent to {@code add(0, e)} but may be more readable for stack-like
     * or deque-like usage patterns.</p>
     * 
     * <p>Note: This operation requires shifting all existing elements and has O(n) time complexity.
     * For frequent additions at the beginning, consider using a different data structure.</p>
     *
     * @param e the byte value to add at the beginning
     */
    public void addFirst(final byte e) {
        add(0, e);
    }

    /**
     * Appends the specified element to the end of this list.
     * 
     * <p>This method adds the element at the end of the list, which is equivalent to {@code add(e)}.
     * This method is provided for API consistency with other list-like structures and for clarity
     * in deque-like usage patterns.</p>
     * 
     * <p>This operation typically runs in amortized constant time, growing the internal array as
     * needed.</p>
     *
     * @param e the byte value to add at the end
     */
    public void addLast(final byte e) {
        add(e);
    }

    /**
     * Removes and returns the first element from this list.
     * 
     * <p>This method removes the element at index 0 and shifts all remaining elements one position
     * to the left. The removed element is returned. This is useful for queue-like or deque-like
     * usage patterns where you process elements in FIFO order.</p>
     * 
     * <p>Note: This operation requires shifting all remaining elements and has O(n) time complexity.
     * For frequent removals from the beginning, consider using a different data structure.</p>
     *
     * @return the byte value that was removed from the beginning
     * @throws NoSuchElementException if the list is empty
     */
    public byte removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(0);
    }

    /**
     * Removes and returns the last element from this list.
     * 
     * <p>This method removes the element at the last position and returns it. This is useful for
     * stack-like or deque-like usage patterns where you process elements in LIFO order. Unlike
     * removing from the beginning, this operation is O(1) as no elements need to be shifted.</p>
     *
     * @return the byte value that was removed from the end
     * @throws NoSuchElementException if the list is empty
     */
    public byte removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(size - 1);
    }

    /**
     * Returns a hash code value for this list.
     * 
     * <p>The hash code is computed based on all elements in the list and their order. Two ByteLists
     * are guaranteed to have the same hash code if they contain the same elements in the same order.
     * The hash code is computed using a algorithm similar to that specified by List.hashCode(),
     * adapted for primitive byte values.</p>
     * 
     * <p>Note: The hash code will change if the list is modified. Do not use mutable ByteLists as
     * keys in hash-based collections.</p>
     *
     * @return a hash code value for this list
     */
    @Override
    public int hashCode() {
        return N.hashCode(elementData, 0, size);
    }

    /**
     * Compares this list with the specified object for equality.
     * 
     * <p>Returns {@code true} if and only if the specified object is also a ByteList, both lists
     * have the same size, and all corresponding pairs of elements are equal. The comparison is
     * performed element by element in order.</p>
     * 
     * <p>This method provides a fast path for ByteList-to-ByteList comparison and properly handles
     * all edge cases including null and self-comparison.</p>
     *
     * @param obj the object to be compared for equality with this list
     * @return {@code true} if the specified object is equal to this list, {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ByteList other) {
            return size == other.size && N.equals(elementData, 0, other.elementData, 0, size);
        }

        return false;
    }

    /**
     * Returns a string representation of this list.
     * 
     * <p>The string representation consists of the list's elements, separated by commas and enclosed
     * in square brackets ("[]"). Adjacent elements are separated by ", " (comma and space). Elements
     * are converted to strings as by {@code String.valueOf(byte)}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList.of().toString()           // Returns "[]"
     * ByteList.of(1).toString()          // Returns "[1]"
     * ByteList.of(1, 2, 3).toString()    // Returns "[1, 2, 3]"
     * }</pre>
     *
     * @return a string representation of this list
     */
    @Override
    public String toString() {
        return size == 0 ? Strings.STR_FOR_EMPTY_ARRAY : N.toString(elementData, 0, size);
    }

    private void ensureCapacity(final int minCapacity) {
        if (minCapacity < 0 || minCapacity > MAX_ARRAY_SIZE) {
            throw new OutOfMemoryError();
        }

        if (N.isEmpty(elementData)) {
            elementData = new byte[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            final int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}

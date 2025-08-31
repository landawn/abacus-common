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
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.stream.IntStream;

/**
 * A resizable, primitive int array implementation of List interface. This class provides
 * methods to manipulate the size of the array that is used internally to store the list.
 * 
 * <p>This implementation provides constant time performance for basic operations (get and set),
 * and amortized constant time for add operations. The size, isEmpty, iterator, and stream
 * operations run in constant time. All other operations run in linear time (roughly speaking).
 * 
 * <p>Each IntList instance has a capacity. The capacity is the size of the array used to store
 * the elements in the list. It is always at least as large as the list size. As elements are
 * added to an IntList, its capacity grows automatically.
 * 
 * <p>An application can increase the capacity of an IntList instance before adding a large
 * number of elements using the ensureCapacity operation. This may reduce the amount of
 * incremental reallocation.
 * 
 * <p><strong>Note that this implementation is not synchronized.</strong> If multiple threads
 * access an IntList instance concurrently, and at least one of the threads modifies the list
 * structurally, it must be synchronized externally.
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 */
public final class IntList extends PrimitiveList<Integer, int[], IntList> {

    @Serial
    private static final long serialVersionUID = 8661773953226671696L;

    static final Random RAND = new SecureRandom();

    private int[] elementData = N.EMPTY_INT_ARRAY;

    private int size = 0;

    /**
     * Constructs an empty IntList with an initial capacity of zero.
     * The internal array will be initialized to an empty array and will grow
     * as needed when elements are added.
     */
    public IntList() {
    }

    /**
     * Constructs an empty IntList with the specified initial capacity.
     * This constructor is useful when the approximate size of the list is known
     * in advance, as it can help avoid multiple array reallocations during element addition.
     *
     * @param initialCapacity the initial capacity of the list. Must be non-negative.
     * @throws IllegalArgumentException if the specified initial capacity is negative
     * @throws OutOfMemoryError if the requested array size exceeds the maximum array size
     */
    public IntList(final int initialCapacity) {
        N.checkArgNotNegative(initialCapacity, cs.initialCapacity);

        elementData = initialCapacity == 0 ? N.EMPTY_INT_ARRAY : new int[initialCapacity];
    }

    /**
     * Constructs an IntList containing all elements from the specified array.
     * The IntList directly uses the provided array as its internal storage without copying,
     * making this constructor very efficient. The size of the list will be equal to the
     * length of the array.
     * 
     * <p><b>Note:</b> Since the array is used directly, any external modifications to the
     * array will affect this list and vice versa.
     *
     * @param a the array whose elements are to be placed into this list. Must not be null.
     */
    public IntList(final int[] a) {
        this(N.requireNonNull(a), a.length);
    }

    /**
     * Constructs an IntList using the specified array as the element array for this list
     * without copying. This allows creating a list that uses only a portion of the array.
     * The list will contain the first 'size' elements from the array.
     * 
     * <p><b>Note:</b> Since the array is used directly, any external modifications to the
     * array will affect this list and vice versa.
     *
     * @param a the array to be used as the element array for this list. Must not be null.
     * @param size the number of elements in the list. Must be between 0 and a.length (inclusive).
     * @throws IndexOutOfBoundsException if size is negative or greater than a.length
     */
    public IntList(final int[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates a new IntList containing all elements from the specified array.
     * If the array is null, an empty IntList is returned. Unlike the constructor,
     * this method creates a new IntList that uses the array directly without copying.
     *
     * @param a the array of integers to be placed into the new list. May be null.
     * @return a new IntList containing the elements of the specified array, or an empty list if the array is null
     */
    public static IntList of(final int... a) {
        return new IntList(N.nullToEmpty(a));
    }

    /**
     * Creates a new IntList containing the specified number of elements from the given array.
     * The list will contain the first 'size' elements from the array. If the array is null,
     * it is treated as an empty array.
     *
     * @param a the array of integers to be placed into the new list. May be null.
     * @param size the number of elements to include in the list. Must be non-negative.
     * @return a new IntList containing the specified number of elements from the array
     * @throws IndexOutOfBoundsException if size is negative or greater than the array length
     */
    public static IntList of(final int[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new IntList(N.nullToEmpty(a), size);
    }

    /**
     * Creates a new IntList that is a copy of the specified array.
     * This method creates a defensive copy of the array, so subsequent modifications
     * to the original array will not affect the returned list.
     *
     * @param a the array to be copied. May be null, in which case an empty list is returned.
     * @return a new IntList containing a copy of the elements from the specified array
     */
    public static IntList copyOf(final int[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates a new IntList that is a copy of the specified range of the array.
     * This method creates a defensive copy of the specified portion of the array.
     *
     * @param a the array from which a range is to be copied
     * @param fromIndex the initial index of the range to be copied, inclusive
     * @param toIndex the final index of the range to be copied, exclusive
     * @return a new IntList containing the elements from the specified range
     * @throws IndexOutOfBoundsException if fromIndex < 0 or toIndex > a.length or fromIndex > toIndex
     */
    public static IntList copyOf(final int[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates a new IntList containing a sequence of integers from startInclusive (inclusive)
     * to endExclusive (exclusive) with a step of 1. For example, range(1, 5) returns [1, 2, 3, 4].
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a new IntList containing integers from startInclusive to endExclusive-1
     */
    public static IntList range(final int startInclusive, final int endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     * Creates a new IntList containing a sequence of integers from startInclusive (inclusive)
     * to endExclusive (exclusive), incremented by the specified step value.
     * 
     * <p>Examples:
     * <ul>
     * <li>range(0, 10, 2) returns [0, 2, 4, 6, 8]</li>
     * <li>range(10, 0, -2) returns [10, 8, 6, 4, 2]</li>
     * <li>range(1, 5, 0) throws IllegalArgumentException</li>
     * </ul>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param by the step value for incrementing. Must not be zero.
     * @return a new IntList containing the sequence of integers
     * @throws IllegalArgumentException if by is zero
     */
    public static IntList range(final int startInclusive, final int endExclusive, final int by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     * Creates a new IntList containing a sequence of integers from startInclusive
     * to endInclusive (both inclusive) with a step of 1.
     * For example, rangeClosed(1, 4) returns [1, 2, 3, 4].
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a new IntList containing integers from startInclusive to endInclusive
     */
    public static IntList rangeClosed(final int startInclusive, final int endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     * Creates a new IntList containing a sequence of integers from startInclusive
     * to endInclusive (both inclusive), incremented by the specified step value.
     * 
     * <p>Examples:
     * <ul>
     * <li>rangeClosed(0, 10, 2) returns [0, 2, 4, 6, 8, 10]</li>
     * <li>rangeClosed(10, 0, -2) returns [10, 8, 6, 4, 2, 0]</li>
     * <li>rangeClosed(1, 5, 0) throws IllegalArgumentException</li>
     * </ul>
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @param by the step value for incrementing. Must not be zero.
     * @return a new IntList containing the sequence of integers
     * @throws IllegalArgumentException if by is zero
     */
    public static IntList rangeClosed(final int startInclusive, final int endInclusive, final int by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     * Creates a new IntList containing the specified element repeated the given number of times.
     * For example, repeat(5, 3) returns [5, 5, 5].
     *
     * @param element the int value to be repeated
     * @param len the number of times to repeat the element. Must be non-negative.
     * @return a new IntList containing the repeated elements
     * @throws IllegalArgumentException if len is negative
     */
    public static IntList repeat(final int element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates a new IntList filled with random int values. Each element is generated
     * using Random.nextInt(), which produces values across the entire range of int
     * (from Integer.MIN_VALUE to Integer.MAX_VALUE).
     *
     * @param len the number of random elements to generate. Must be non-negative.
     * @return a new IntList containing random int values
     * @throws IllegalArgumentException if len is negative
     * @see Random#nextInt()
     * @see Array#random(int)
     */
    public static IntList random(final int len) {
        final int[] a = new int[len];

        for (int i = 0; i < len; i++) {
            a[i] = RAND.nextInt();
        }

        return of(a);
    }

    /**
     * Creates a new IntList filled with random int values within the specified range.
     * Each element is randomly generated to be greater than or equal to startInclusive
     * and less than endExclusive.
     * 
     * <p>The distribution of values is uniform across the specified range.
     *
     * @param startInclusive the lower bound (inclusive) for the random values
     * @param endExclusive the upper bound (exclusive) for the random values
     * @param len the number of random elements to generate. Must be non-negative.
     * @return a new IntList containing random values within the specified range
     * @throws IllegalArgumentException if startInclusive >= endExclusive or if len is negative
     * @see Random#nextInt(int)
     * @see Array#random(int, int, int)
     */
    public static IntList random(final int startInclusive, final int endExclusive, final int len) {
        if (startInclusive >= endExclusive) {
            throw new IllegalArgumentException("'startInclusive' must be less than 'endExclusive'");
        }

        final int[] a = new int[len];
        final long mod = (long) endExclusive - (long) startInclusive;

        if (mod < Integer.MAX_VALUE) {
            final int n = (int) mod;

            for (int i = 0; i < len; i++) {
                a[i] = RAND.nextInt(n) + startInclusive;
            }
        } else {
            for (int i = 0; i < len; i++) {
                a[i] = (int) (Math.abs(RAND.nextLong() % mod) + startInclusive);
            }
        }

        return of(a);
    }

    /**
     * Returns the internal array backing this list without creating a copy.
     * This method provides direct access to the internal array for performance reasons.
     * 
     * <p><b>Warning:</b> The returned array should not be modified unless you understand
     * the implications. Modifications to the returned array will directly affect this list.
     * The array may be larger than the list size; only elements from index 0 to size()-1
     * are valid list elements.
     *
     * @return the internal array backing this list
     * @deprecated should call {@code toArray()}
     */
    @Beta
    @Deprecated
    @Override
    public int[] array() {
        return elementData;
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index the index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size())
     */
    public int get(final int index) {
        rangeCheck(index);

        return elementData[index];
    }

    /**
     * Checks if the specified index is within the valid range of the list.
     *
     * @param index the index to check
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size())
     */
    private void rangeCheck(final int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @param index the index of the element to replace
     * @param e the element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size())
     */
    public int set(final int index, final int e) {
        rangeCheck(index);

        final int oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     * Appends the specified element to the end of this list.
     * The list will automatically grow if necessary to accommodate the new element.
     *
     * @param e the element to be appended to this list
     */
    public void add(final int e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     *
     * @param index the index at which the specified element is to be inserted
     * @param e the element to be inserted
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > size())
     */
    public void add(final int index, final int e) {
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
     * Appends all elements from the specified IntList to the end of this list,
     * in the order they appear in the specified list.
     *
     * @param c the IntList containing elements to be added to this list
     * @return true if this list changed as a result of the call (i.e., if c was not empty)
     */
    @Override
    public boolean addAll(final IntList c) {
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
     * Inserts all elements from the specified IntList into this list at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to
     * the right (increases their indices). The new elements will appear in this list in the
     * order they appear in the specified list.
     *
     * @param index the index at which to insert the first element from the specified list
     * @param c the IntList containing elements to be inserted into this list
     * @return true if this list changed as a result of the call (i.e., if c was not empty)
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > size())
     */
    @Override
    public boolean addAll(final int index, final IntList c) {
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
     * Appends all elements from the specified array to the end of this list,
     * in the order they appear in the array.
     *
     * @param a the array containing elements to be added to this list
     * @return true if this list changed as a result of the call (i.e., if the array was not empty)
     */
    @Override
    public boolean addAll(final int[] a) {
        return addAll(size(), a);
    }

    /**
     * Inserts all elements from the specified array into this list at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to
     * the right (increases their indices). The new elements will appear in this list in the
     * order they appear in the array.
     *
     * @param index the index at which to insert the first element from the specified array
     * @param a the array containing elements to be inserted into this list
     * @return true if this list changed as a result of the call (i.e., if the array was not empty)
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > size())
     */
    @Override
    public boolean addAll(final int index, final int[] a) {
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
     *
     * @param index the index to check
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > size())
     */
    private void rangeCheckForAdd(final int index) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    /**
     * Removes the first occurrence of the specified element from this list, if it is present.
     * If the list does not contain the element, it is unchanged. More formally, removes the
     * element with the lowest index i such that get(i) == e.
     *
     * @param e the element to be removed from this list, if present
     * @return true if this list contained the specified element and it was removed
     */
    public boolean remove(final int e) {
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
     * The list is compacted after removal, and remaining elements maintain their relative order.
     *
     * @param e the element to be removed from this list
     * @return true if this list was modified (i.e., at least one occurrence was removed)
     */
    public boolean removeAllOccurrences(final int e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] != e) {
                elementData[w++] = elementData[i];
            }
        }

        final int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, 0);

            size = w;
        }

        return numRemoved > 0;
    }

    /**
     * Private helper method to remove an element at the specified index without range checking.
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
     * Removes from this list all of its elements that are contained in the specified IntList.
     * This method compares elements by value, removing all occurrences found in the specified list.
     *
     * @param c the IntList containing elements to be removed from this list
     * @return true if this list was modified as a result of the call
     */
    @Override
    public boolean removeAll(final IntList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        return batchRemove(c, false) > 0;
    }

    /**
     * Removes from this list all of its elements that are contained in the specified array.
     * This method compares elements by value, removing all occurrences found in the array.
     *
     * @param a the array containing elements to be removed from this list
     * @return true if this list was modified as a result of the call
     */
    @Override
    public boolean removeAll(final int[] a) {
        if (N.isEmpty(a)) {
            return false;
        }

        return removeAll(of(a));
    }

    /**
     * Removes all elements from this list that satisfy the given predicate.
     * The elements are tested in order, and those for which the predicate returns true
     * are removed. The relative order of retained elements is preserved.
     *
     * @param p the predicate which returns true for elements to be removed
     * @return true if any elements were removed from this list
     */
    public boolean removeIf(final IntPredicate p) {
        final IntList tmp = new IntList(size());

        for (int i = 0; i < size; i++) {
            if (!p.test(elementData[i])) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, elementData, 0, tmp.size());
        N.fill(elementData, tmp.size(), size, 0);
        size = tmp.size;

        return true;
    }

    /**
     * Removes duplicate elements from this list, keeping only the first occurrence of each value.
     * If the list is sorted, this method uses an optimized algorithm. The relative order of
     * retained elements is preserved.
     *
     * @return true if any duplicates were removed from this list
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
            final Set<Integer> set = N.newLinkedHashSet(size);
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
            N.fill(elementData, idx + 1, size, 0);

            size = idx + 1;
            return true;
        }
    }

    /**
     * Retains only the elements in this list that are contained in the specified IntList.
     * In other words, removes from this list all of its elements that are not contained
     * in the specified list. Elements are compared by value.
     *
     * @param c the IntList containing elements to be retained in this list
     * @return true if this list was modified as a result of the call
     */
    @Override
    public boolean retainAll(final IntList c) {
        if (N.isEmpty(c)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return batchRemove(c, true) > 0;
    }

    /**
     * Retains only the elements in this list that are contained in the specified array.
     * In other words, removes from this list all of its elements that are not contained
     * in the specified array. Elements are compared by value.
     *
     * @param a the array containing elements to be retained in this list
     * @return true if this list was modified as a result of the call
     */
    @Override
    public boolean retainAll(final int[] a) {
        if (N.isEmpty(a)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(IntList.of(a));
    }

    /**
     * Helper method for removeAll and retainAll operations.
     *
     * @param c the collection to use for comparison
     * @param complement if true, retains elements in c; if false, removes elements in c
     * @return the number of elements removed
     */
    private int batchRemove(final IntList c, final boolean complement) {
        final int[] elementData = this.elementData;//NOSONAR

        int w = 0;

        if (c.size() > 3 && size() > 9) {
            final Set<Integer> set = c.toSet();

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
            N.fill(elementData, w, size, 0);

            size = w;
        }

        return numRemoved;
    }

    /**
     * Removes the element at the specified position in this list and returns it.
     * Shifts any subsequent elements to the left (subtracts one from their indices).
     *
     * @param index the index of the element to be removed
     * @return the element that was removed from the list
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size())
     */
    public int delete(final int index) {
        rangeCheck(index);

        final int oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     * Removes the elements at the specified positions from this list.
     * The indices array is processed to remove elements efficiently, handling
     * duplicate indices and maintaining the correct element positions during removal.
     *
     * @param indices the indices of elements to be removed. Null or empty array results in no change.
     */
    @Override
    public void deleteAllByIndices(final int... indices) {
        if (N.isEmpty(indices)) {
            return;
        }

        final int[] tmp = N.deleteAllByIndices(elementData, indices);

        N.copy(tmp, 0, elementData, 0, tmp.length);

        if (size > tmp.length) {
            N.fill(elementData, tmp.length, size, (char) 0);
        }

        size -= elementData.length - tmp.length;
    }

    /**
     * Removes from this list all elements whose index is between fromIndex (inclusive)
     * and toIndex (exclusive). Shifts any succeeding elements to the left (reduces their index).
     *
     * @param fromIndex the index of the first element to be removed (inclusive)
     * @param toIndex the index after the last element to be removed (exclusive)
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         (fromIndex < 0 || toIndex > size() || fromIndex > toIndex)
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

        N.fill(elementData, newSize, size, 0);

        this.size = newSize;
    }

    /**
     * Moves a range of elements within this list to a new position.
     * The elements from fromIndex (inclusive) to toIndex (exclusive) are moved
     * so that the element originally at fromIndex will be at newPositionStartIndexAfterMove.
     * Other elements are shifted as necessary to accommodate the move.
     * 
     * <p>Example: 
     * <pre>
     * IntList list = IntList.of(0, 1, 2, 3, 4, 5);
     * list.moveRange(1, 3, 4);  // Moves elements [1, 2] to position starting at index 4
     * // Result: [0, 3, 4, 1, 2, 5]
     * </pre>
     *
     * @param fromIndex the starting index (inclusive) of the range to be moved
     * @param toIndex the ending index (exclusive) of the range to be moved
     * @param newPositionStartIndexAfterMove the start index where the range should be positioned after the move.
     *        Must be in the range [0, size - (toIndex - fromIndex)]
     * @throws IndexOutOfBoundsException if any index is out of bounds or if the new position is invalid
     */
    @Override
    public void moveRange(final int fromIndex, final int toIndex, final int newPositionStartIndexAfterMove) {
        N.moveRange(elementData, fromIndex, toIndex, newPositionStartIndexAfterMove);
    }

    /**
     * Replaces each element in the specified range of this list with elements from
     * the replacement IntList. The range from fromIndex (inclusive) to toIndex (exclusive)
     * is removed and replaced with all elements from the replacement list.
     * 
     * <p>If the replacement list has a different size than the range being replaced,
     * the list will grow or shrink accordingly.
     *
     * @param fromIndex the starting index (inclusive) of the range to replace
     * @param toIndex the ending index (exclusive) of the range to replace
     * @param replacement the IntList whose elements will replace the specified range
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         (fromIndex < 0 || toIndex > size() || fromIndex > toIndex)
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final IntList replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, 0);
        }

        this.size = newSize;
    }

    /**
     * Replaces each element in the specified range of this list with elements from
     * the replacement array. The range from fromIndex (inclusive) to toIndex (exclusive)
     * is removed and replaced with all elements from the replacement array.
     * 
     * <p>If the replacement array has a different length than the range being replaced,
     * the list will grow or shrink accordingly.
     *
     * @param fromIndex the starting index (inclusive) of the range to replace
     * @param toIndex the ending index (exclusive) of the range to replace
     * @param replacement the array whose elements will replace the specified range
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         (fromIndex < 0 || toIndex > size() || fromIndex > toIndex)
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final int[] replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, 0);
        }

        this.size = newSize;
    }

    /**
     * Replaces all occurrences of the specified value in this list with the new value.
     *
     * @param oldVal the value to be replaced
     * @param newVal the value to replace oldVal
     * @return the number of elements that were replaced
     */
    public int replaceAll(final int oldVal, final int newVal) {
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
     * Replaces each element of this list with the result of applying the specified operator
     * to that element. The operator is applied to each element in order.
     *
     * @param operator the operator to apply to each element
     */
    public void replaceAll(final IntUnaryOperator operator) {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsInt(elementData[i]);
        }
    }

    /**
     * Replaces all elements in this list that satisfy the given predicate with the specified new value.
     *
     * @param predicate the predicate which returns true for elements to be replaced
     * @param newValue the value to replace matching elements with
     * @return true if at least one element was replaced
     */
    public boolean replaceIf(final IntPredicate predicate, final int newValue) {
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
     *
     * @param val the value to be stored in all elements of the list
     */
    public void fill(final int val) {
        fill(0, size(), val);
    }

    /**
     * Replaces each element in the specified range of this list with the specified value.
     *
     * @param fromIndex the index of the first element (inclusive) to be filled with the specified value
     * @param toIndex the index after the last element (exclusive) to be filled with the specified value
     * @param val the value to be stored in the specified range of the list
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         (fromIndex < 0 || toIndex > size() || fromIndex > toIndex)
     */
    public void fill(final int fromIndex, final int toIndex, final int val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     * Returns true if this list contains the specified element.
     * More formally, returns true if and only if this list contains at least one
     * element e such that e == valueToFind.
     *
     * @param valueToFind the element whose presence in this list is to be tested
     * @return true if this list contains the specified element
     */
    public boolean contains(final int valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     * Returns true if this list contains any element that is also contained in the
     * specified IntList. This method returns true if the two lists share at least
     * one common element.
     *
     * @param c the IntList to be checked for containment in this list
     * @return true if this list contains any element from the specified list
     */
    @Override
    public boolean containsAny(final IntList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    /**
     * Returns true if this list contains any element that is also contained in the
     * specified array. This method returns true if this list and the array share
     * at least one common element.
     *
     * @param a the array to be checked for containment in this list
     * @return true if this list contains any element from the specified array
     */
    @Override
    public boolean containsAny(final int[] a) {
        if (isEmpty() || N.isEmpty(a)) {
            return false;
        }

        return !disjoint(a);
    }

    /**
     * Returns true if this list contains all elements in the specified IntList.
     * This method returns true if the specified list is a subset of this list
     * (ignoring element order but considering duplicates).
     *
     * @param c the IntList to be checked for containment in this list
     * @return true if this list contains all elements in the specified list
     */
    @Override
    public boolean containsAll(final IntList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        if (needToSet(size(), c.size())) {
            final Set<Integer> set = this.toSet();

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
     * Returns true if this list contains all elements in the specified array.
     * This method returns true if all elements in the array are present in this list
     * (ignoring element order but considering duplicates).
     *
     * @param a the array to be checked for containment in this list
     * @return true if this list contains all elements in the specified array
     */
    @Override
    public boolean containsAll(final int[] a) {
        if (N.isEmpty(a)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        return containsAll(of(a));
    }

    /**
     * Returns true if this list has no elements in common with the specified IntList.
     * Two lists are disjoint if they share no common elements.
     *
     * @param c the IntList to check for disjointness with this list
     * @return true if the two lists have no elements in common
     */
    @Override
    public boolean disjoint(final IntList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        if (needToSet(size(), c.size())) {
            final Set<Integer> set = this.toSet();

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
     * Returns true if this list has no elements in common with the specified array.
     * This list and the array are disjoint if they share no common elements.
     *
     * @param b the array to check for disjointness with this list
     * @return true if this list and the array have no elements in common
     */
    @Override
    public boolean disjoint(final int[] b) {
        if (isEmpty() || N.isEmpty(b)) {
            return true;
        }

        return disjoint(of(b));
    }

    /**
     * Returns a new list containing elements that are present in both this list and the specified list.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences
     * present in both lists.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * IntList list2 = IntList.of(1, 2, 2, 4);
     * IntList result = list1.intersection(list2); // result will be [1, 2, 2]
     * // One occurrence of '1' (minimum count in both lists) and two occurrences of '2'
     * </pre>
     *
     * @param b the list to find common elements with this list
     * @return a new IntList containing elements present in both lists, considering the minimum
     *         number of occurrences in either list. Returns an empty list if either list is empty.
     * @see #intersection(int[])
     * @see #difference(IntList)
     * @see #symmetricDifference(IntList)
     */
    @Override
    public IntList intersection(final IntList b) {
        if (N.isEmpty(b)) {
            return new IntList();
        }

        final Multiset<Integer> bOccurrences = b.toMultiset();

        final IntList c = new IntList(N.min(9, size(), b.size()));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Returns a new list containing elements that are present in both this list and the specified array.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences
     * present in both sources.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * int[] array = new int[]{1, 2, 2, 4};
     * IntList result = list1.intersection(array); // result will be [1, 2, 2]
     * </pre>
     *
     * @param b the array to find common elements with this list
     * @return a new IntList containing elements present in both this list and the array,
     *         considering the minimum number of occurrences. Returns an empty list if the array is empty.
     * @see #intersection(IntList)
     * @see #difference(int[])
     * @see #symmetricDifference(int[])
     */
    @Override
    public IntList intersection(final int[] b) {
        if (N.isEmpty(b)) {
            return new IntList();
        }

        return intersection(of(b));
    }

    /**
     * Returns a new list containing elements that are in this list but not in the specified list,
     * considering the number of occurrences of each element. If an element appears multiple times
     * in both lists, the difference will contain the extra occurrences from this list.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * IntList list2 = IntList.of(2, 5, 1);
     * IntList result = list1.difference(list2); // result will be [0, 2, 3]
     * // One '2' remains because list1 has two occurrences and list2 has one
     * </pre>
     *
     * @param b the list whose elements are to be removed from this list
     * @return a new IntList containing elements present in this list but not in the specified list,
     *         considering the number of occurrences
     * @see #difference(int[])
     * @see #symmetricDifference(IntList)
     * @see #intersection(IntList)
     */
    @Override
    public IntList difference(final IntList b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Integer> bOccurrences = b.toMultiset();

        final IntList c = new IntList(N.min(size(), N.max(9, size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (!bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Returns a new list containing elements that are in this list but not in the specified array,
     * considering the number of occurrences of each element. If an element appears multiple times
     * in both sources, the difference will contain the extra occurrences from this list.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * int[] array = new int[]{2, 5, 1};
     * IntList result = list1.difference(array); // result will be [0, 2, 3]
     * </pre>
     *
     * @param b the array whose elements are to be removed from this list
     * @return a new IntList containing elements present in this list but not in the array,
     *         considering the number of occurrences. Returns a copy of this list if the array is empty.
     * @see #difference(IntList)
     * @see #symmetricDifference(int[])
     * @see #intersection(int[])
     */
    @Override
    public IntList difference(final int[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(b));
    }

    /**
     * Returns a new IntList containing elements that are present in either this list or the specified list,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the result contains the absolute difference
     * in the number of occurrences.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified list.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * IntList list2 = IntList.of(2, 5, 1);
     * IntList result = list1.symmetricDifference(list2);
     * // result will contain: [0, 2, 3, 5]
     * // Elements explanation:
     * // - 0, 3: only in list1
     * // - 5: only in list2
     * // - 2: appears twice in list1 and once in list2, so one occurrence remains
     * </pre>
     *
     * @param b the list to compare with this list for symmetric difference
     * @return a new IntList containing elements that are in either list but not in both,
     *         considering the number of occurrences
     * @see #symmetricDifference(int[])
     * @see #difference(IntList)
     * @see #intersection(IntList)
     */
    @Override
    public IntList symmetricDifference(final IntList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (isEmpty()) {
            return b.copy();
        }

        final Multiset<Integer> bOccurrences = b.toMultiset();
        final IntList c = new IntList(N.max(9, Math.abs(size() - b.size())));

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
     * Returns a new IntList containing elements that are present in either this list or the specified array,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the result contains the absolute difference
     * in the number of occurrences.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * int[] array = new int[]{2, 5, 1};
     * IntList result = list1.symmetricDifference(array);
     * // result will contain: [0, 2, 3, 5]
     * </pre>
     *
     * @param b the array to compare with this list for symmetric difference
     * @return a new IntList containing elements that are in either this list or the array but not in both,
     *         considering the number of occurrences
     * @see #symmetricDifference(IntList)
     * @see #difference(int[])
     * @see #intersection(int[])
     */
    @Override
    public IntList symmetricDifference(final int[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        } else if (isEmpty()) {
            return of(N.copyOfRange(b, 0, b.length));
        }

        return symmetricDifference(of(b));
    }

    /**
     * Returns the number of times the specified value appears in this list.
     *
     * @param valueToFind the value whose frequency is to be determined
     * @return the number of times the value appears in this list
     */
    public int occurrencesOf(final int valueToFind) {
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
     * Returns the index of the first occurrence of the specified element in this list,
     * or -1 if this list does not contain the element. The search starts from index 0.
     *
     * @param valueToFind the element to search for
     * @return the index of the first occurrence of the element, or -1 if not found
     */
    public int indexOf(final int valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * searching forwards from the specified index, or -1 if the element is not found.
     *
     * @param valueToFind the element to search for
     * @param fromIndex the index to start searching from (inclusive)
     * @return the index of the first occurrence of the element at or after fromIndex,
     *         or -1 if not found
     */
    public int indexOf(final int valueToFind, final int fromIndex) {
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
     * Returns the index of the last occurrence of the specified element in this list,
     * or -1 if this list does not contain the element. The search starts from the end
     * of the list and proceeds backwards.
     *
     * @param valueToFind the element to search for
     * @return the index of the last occurrence of the element, or -1 if not found
     */
    public int lastIndexOf(final int valueToFind) {
        return lastIndexOf(valueToFind, size);
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * searching backwards from the specified index, or -1 if the element is not found.
     *
     * @param valueToFind the element to search for
     * @param startIndexFromBack the index to start searching backwards from (inclusive).
     *        The search includes this index if it's within bounds.
     * @return the index of the last occurrence of the element at or before startIndexFromBack,
     *         or -1 if not found
     */
    public int lastIndexOf(final int valueToFind, final int startIndexFromBack) {
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
     * Returns the minimum element in this list wrapped in an OptionalInt.
     * If the list is empty, returns an empty OptionalInt.
     *
     * @return an OptionalInt containing the minimum element, or an empty OptionalInt if the list is empty
     */
    public OptionalInt min() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(N.min(elementData, 0, size));
    }

    /**
     * Returns the minimum element in the specified range of this list wrapped in an OptionalInt.
     * If the range is empty (fromIndex equals toIndex), returns an empty OptionalInt.
     *
     * @param fromIndex the index of the first element (inclusive) in the range
     * @param toIndex the index after the last element (exclusive) in the range
     * @return an OptionalInt containing the minimum element in the range, or an empty OptionalInt if the range is empty
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         (fromIndex < 0 || toIndex > size() || fromIndex > toIndex)
     */
    public OptionalInt min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalInt.empty() : OptionalInt.of(N.min(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the maximum element in this list wrapped in an OptionalInt.
     * If the list is empty, returns an empty OptionalInt.
     *
     * @return an OptionalInt containing the maximum element, or an empty OptionalInt if the list is empty
     */
    public OptionalInt max() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(N.max(elementData, 0, size));
    }

    /**
     * Returns the maximum element in the specified range of this list wrapped in an OptionalInt.
     * If the range is empty (fromIndex equals toIndex), returns an empty OptionalInt.
     *
     * @param fromIndex the index of the first element (inclusive) in the range
     * @param toIndex the index after the last element (exclusive) in the range
     * @return an OptionalInt containing the maximum element in the range, or an empty OptionalInt if the range is empty
     * @throws IndexOutOfBoundsException if fromIndex or toIndex is out of range
     *         (fromIndex < 0 || toIndex > size() || fromIndex > toIndex)
     */
    public OptionalInt max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalInt.empty() : OptionalInt.of(N.max(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the median value of all elements in this list.
     * 
     * <p>The median is the middle value when the elements are sorted in ascending order. For lists with
     * an odd number of elements, this is the exact middle element. For lists with an even number of
     * elements, this method returns the lower of the two middle elements (not the average).</p>
     *
     * @return an OptionalInt containing the median value if the list is non-empty, or an empty OptionalInt if the list is empty
     */
    public OptionalInt median() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(N.median(elementData, 0, size));
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
     * @return an OptionalInt containing the median value if the range is non-empty, or an empty OptionalInt if the range is empty
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()} or {@code fromIndex > toIndex}
     */
    public OptionalInt median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalInt.empty() : OptionalInt.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     * Performs the given action for each element in this list in sequential order.
     * The action is executed once for each element, passing the element value as the argument.
     *
     * @param action the action to be performed for each element
     */
    public void forEach(final IntConsumer action) {
        forEach(0, size, action);
    }

    /**
     * Performs the given action for each element in the specified range of this list.
     * The action is executed once for each element in the range, passing the element value as the argument.
     * 
     * <p>If fromIndex is less than toIndex, elements are processed in forward order from fromIndex
     * (inclusive) to toIndex (exclusive). If fromIndex is greater than toIndex, elements are
     * processed in reverse order from fromIndex (inclusive) down to toIndex (exclusive).
     * If toIndex is -1, it is treated as 0 for reverse iteration.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to iterate
     * @param toIndex the ending index (exclusive) of the range to iterate, or -1 for reverse iteration from fromIndex to start
     * @param action the action to be performed for each element
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public void forEach(final int fromIndex, final int toIndex, final IntConsumer action) throws IndexOutOfBoundsException {
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
     * Returns the first element in this list wrapped in an OptionalInt.
     * 
     * @return an OptionalInt containing the first element if the list is not empty,
     *         or an empty OptionalInt if the list is empty
     */
    public OptionalInt first() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(elementData[0]);
    }

    /**
     * Returns the last element in this list wrapped in an OptionalInt.
     * 
     * @return an OptionalInt containing the last element if the list is not empty,
     *         or an empty OptionalInt if the list is empty
     */
    public OptionalInt last() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(elementData[size() - 1]);
    }

    /**
     * Returns a new IntList containing only the distinct elements from the specified range
     * of this list. The order of elements is preserved, with the first occurrence of each
     * distinct value being retained.
     * 
     * <p>This method uses an efficient algorithm to identify distinct elements without
     * modifying the original list.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to process
     * @param toIndex the ending index (exclusive) of the range to process
     * @return a new IntList containing only distinct elements from the specified range
     * @throws IndexOutOfBoundsException if fromIndex < 0, toIndex > size(), or fromIndex > toIndex
     */
    @Override
    public IntList distinct(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            return of(N.distinct(elementData, fromIndex, toIndex));
        } else {
            return of(N.copyOfRange(elementData, fromIndex, toIndex));
        }
    }

    /**
     * Checks whether this list contains any duplicate elements.
     * An element is considered a duplicate if it appears more than once in the list.
     *
     * @return true if the list contains at least one duplicate element, false otherwise
     */
    @Override
    public boolean hasDuplicates() {
        return N.hasDuplicates(elementData, 0, size, false);
    }

    /**
     * Checks whether the elements in this list are sorted in ascending order.
     * An empty list or a list with a single element is considered sorted.
     *
     * @return true if all elements are in ascending order (allowing equal consecutive values),
     *         false otherwise
     */
    @Override
    public boolean isSorted() {
        return N.isSorted(elementData, 0, size);
    }

    /**
     * Sorts all elements in this list in ascending order.
     * This method modifies the list in place using an efficient sorting algorithm.
     * 
     * <p>The sorting algorithm used is typically a dual-pivot quicksort which offers
     * O(n log n) performance on average.</p>
     */
    @Override
    public void sort() {
        if (size > 1) {
            N.sort(elementData, 0, size);
        }
    }

    /**
     * Sorts all elements in this list in ascending order using a parallel sorting algorithm.
     * This method modifies the list in place and may offer better performance than sort()
     * for large lists on multi-core systems.
     * 
     * <p>The parallel sorting algorithm divides the list into sub-arrays which are sorted
     * in parallel and then merged. For small lists, it may fall back to sequential sorting.</p>
     */
    public void parallelSort() {
        if (size > 1) {
            N.parallelSort(elementData, 0, size);
        }
    }

    /**
     * Sorts all elements in this list in descending order.
     * This method first sorts the list in ascending order, then reverses it.
     * The list is modified in place.
     */
    @Override
    public void reverseSort() {
        if (size > 1) {
            sort();
            reverse();
        }
    }

    /**
     * Searches for the specified value in this list using binary search algorithm.
     * The list must be sorted in ascending order prior to making this call.
     * If the list is not sorted, the results are undefined.
     * 
     * <p>If the list contains multiple elements equal to the specified value,
     * there is no guarantee which one will be found.</p>
     *
     * @param valueToFind the value to search for
     * @return the index of the search key if it is contained in the list;
     *         otherwise, (-(insertion point) - 1). The insertion point is defined
     *         as the point at which the key would be inserted into the list
     */
    public int binarySearch(final int valueToFind) {
        return N.binarySearch(elementData, valueToFind);
    }

    /**
     * Searches for the specified value in the specified range of this list using binary search algorithm.
     * The range must be sorted in ascending order prior to making this call.
     * If the range is not sorted, the results are undefined.
     * 
     * <p>If the range contains multiple elements equal to the specified value,
     * there is no guarantee which one will be found.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to search
     * @param toIndex the ending index (exclusive) of the range to search
     * @param valueToFind the value to search for
     * @return the index of the search key if it is contained in the specified range;
     *         otherwise, (-(insertion point) - 1). The insertion point is defined
     *         as the point at which the key would be inserted into the range
     * @throws IndexOutOfBoundsException if fromIndex < 0, toIndex > size(), or fromIndex > toIndex
     */
    public int binarySearch(final int fromIndex, final int toIndex, final int valueToFind) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return N.binarySearch(elementData, fromIndex, toIndex, valueToFind);
    }

    /**
     * Reverses the order of all elements in this list.
     * After this method returns, the first element becomes the last,
     * the second element becomes the second to last, and so on.
     * This method modifies the list in place.
     */
    @Override
    public void reverse() {
        if (size > 1) {
            N.reverse(elementData, 0, size);
        }
    }

    /**
     * Reverses the order of elements in the specified range of this list.
     * After this method returns, the element at fromIndex becomes the element
     * at (toIndex - 1), and vice versa. Elements outside the specified range
     * are not affected. This method modifies the list in place.
     *
     * @param fromIndex the starting index (inclusive) of the range to reverse
     * @param toIndex the ending index (exclusive) of the range to reverse
     * @throws IndexOutOfBoundsException if fromIndex < 0, toIndex > size(), or fromIndex > toIndex
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
     * After this method returns, the elements will be in a random order.
     * This method uses a default source of randomness and modifies the list in place.
     * 
     * <p>This implementation uses the Fisher-Yates shuffle algorithm which
     * guarantees that all permutations are equally likely.</p>
     */
    @Override
    public void shuffle() {
        if (size() > 1) {
            N.shuffle(elementData, 0, size);
        }
    }

    /**
     * Randomly shuffles all elements in this list using the specified source of randomness.
     * After this method returns, the elements will be in a random order determined by
     * the given Random object. This method modifies the list in place.
     * 
     * <p>This implementation uses the Fisher-Yates shuffle algorithm which
     * guarantees that all permutations are equally likely, assuming the Random
     * object produces uniformly distributed values.</p>
     *
     * @param rnd the source of randomness to use for shuffling
     */
    @Override
    public void shuffle(final Random rnd) {
        if (size() > 1) {
            N.shuffle(elementData, 0, size, rnd);
        }
    }

    /**
     * Swaps the elements at the specified positions in this list.
     * After this method returns, the element previously at position i will be
     * at position j, and vice versa. This method modifies the list in place.
     *
     * @param i the index of the first element to swap
     * @param j the index of the second element to swap
     * @throws IndexOutOfBoundsException if either i or j is out of range
     *         (i < 0 || i >= size() || j < 0 || j >= size())
     */
    @Override
    public void swap(final int i, final int j) {
        rangeCheck(i);
        rangeCheck(j);

        set(i, set(j, elementData[i]));
    }

    /**
     * Returns a new IntList containing a copy of all elements in this list.
     * The returned list is independent of this list, so changes to the
     * returned list will not affect this list and vice versa.
     *
     * @return a new IntList containing all elements from this list
     */
    @Override
    public IntList copy() {
        return new IntList(N.copyOfRange(elementData, 0, size));
    }

    /**
     * Returns a new IntList containing a copy of elements in the specified range of this list.
     * The returned list is independent of this list, so changes to the
     * returned list will not affect this list and vice versa.
     *
     * @param fromIndex the starting index (inclusive) of the range to copy
     * @param toIndex the ending index (exclusive) of the range to copy
     * @return a new IntList containing the elements in the specified range
     * @throws IndexOutOfBoundsException if fromIndex < 0, toIndex > size(), or fromIndex > toIndex
     */
    @Override
    public IntList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new IntList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     * Returns a new IntList containing a copy of elements from the specified range of this list,
     * selecting only elements at intervals defined by the step parameter.
     * 
     * <p>For example, with step=2, this method returns every second element in the range.
     * If step is negative and fromIndex > toIndex, elements are selected in reverse order.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to copy
     * @param toIndex the ending index (exclusive) of the range to copy
     * @param step the interval between selected elements. Must not be zero.
     *             Positive values select elements in forward direction,
     *             negative values select elements in reverse direction
     * @return a new IntList containing the selected elements
     * @throws IndexOutOfBoundsException if the range is invalid
     * @throws IllegalArgumentException if step is zero
     * @see N#copyOfRange(int[], int, int, int)
     */
    @Override
    public IntList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex));

        return new IntList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Splits this list into consecutive chunks of the specified size and returns them as a List of IntLists.
     * Each chunk (except possibly the last) will have exactly chunkSize elements.
     * The last chunk may have fewer elements if the range size is not evenly divisible by chunkSize.
     * 
     * <p>The returned chunks are independent copies, so modifications to them will not
     * affect this list or each other.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to split
     * @param toIndex the ending index (exclusive) of the range to split
     * @param chunkSize the desired size of each chunk. Must be greater than 0
     * @return a List containing the IntList chunks
     * @throws IndexOutOfBoundsException if fromIndex < 0, toIndex > size(), or fromIndex > toIndex
     * @throws IllegalArgumentException if chunkSize <= 0
     */
    @Override
    public List<IntList> split(final int fromIndex, final int toIndex, final int chunkSize) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<int[]> list = N.split(elementData, fromIndex, toIndex, chunkSize);
        @SuppressWarnings("rawtypes")
        final List<IntList> result = (List) list;

        for (int i = 0, len = list.size(); i < len; i++) {
            result.set(i, of(list.get(i)));
        }

        return result;
    }

    /**
     * Trims the capacity of this IntList instance to be the list's current size.
     * This method can be used to minimize the storage of an IntList instance.
     * If the capacity is already equal to the size, this method does nothing.
     * 
     * <p>After this call, the capacity of the list will be equal to its size,
     * eliminating any unused capacity.</p>
     *
     * @return this IntList instance (for method chaining)
     */
    @Override
    public IntList trimToSize() {
        if (elementData.length > size) {
            elementData = N.copyOfRange(elementData, 0, size);
        }

        return this;
    }

    /**
     * Removes all elements from this list.
     * The list will be empty after this call returns.
     * The capacity of the list is not changed.
     * 
     * <p>This implementation also clears the underlying array to allow
     * garbage collection of any referenced objects.</p>
     */
    @Override
    public void clear() {
        if (size > 0) {
            N.fill(elementData, 0, size, 0);
        }

        size = 0;
    }

    /**
     * Returns true if this list contains no elements.
     *
     * @return true if this list contains no elements, false otherwise
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
     * Returns a List containing all elements in this list converted to Integer objects.
     * The returned list is a new ArrayList and is independent of this list.
     * 
     * <p>This method is useful when you need to work with APIs that require
     * List<Integer> rather than primitive int arrays.</p>
     *
     * @return a new List<Integer> containing all elements from this list
     */
    @Override
    public List<Integer> boxed() {
        return boxed(0, size);
    }

    /**
     * Returns a List containing elements from the specified range of this list
     * converted to Integer objects. The returned list is a new ArrayList and
     * is independent of this list.
     * 
     * <p>This method is useful when you need to work with APIs that require
     * List<Integer> rather than primitive int arrays.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to box
     * @param toIndex the ending index (exclusive) of the range to box
     * @return a new List<Integer> containing elements from the specified range
     * @throws IndexOutOfBoundsException if fromIndex < 0, toIndex > size(), or fromIndex > toIndex
     */
    @Override
    public List<Integer> boxed(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Integer> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    /**
     * Returns a new array containing all elements in this list.
     * The returned array is independent of this list, so changes to the
     * returned array will not affect this list and vice versa.
     *
     * @return a new int array containing all elements from this list
     */
    @Override
    public int[] toArray() {
        return N.copyOfRange(elementData, 0, size);
    }

    /**
     * Converts this IntList to a LongList.
     * Each int value is widened to a long value without loss of information.
     * The returned LongList is independent of this list.
     *
     * @return a new LongList containing all elements from this list converted to long values
     */
    public LongList toLongList() {
        final long[] a = new long[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i];//NOSONAR
        }

        return LongList.of(a);
    }

    /**
     * Converts this IntList to a FloatList.
     * Each int value is converted to a float value. Note that for large int values,
     * there may be a loss of precision in the conversion.
     * The returned FloatList is independent of this list.
     *
     * @return a new FloatList containing all elements from this list converted to float values
     */
    public FloatList toFloatList() {
        final float[] a = new float[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i];//NOSONAR
        }

        return FloatList.of(a);
    }

    /**
     * Converts this IntList to a DoubleList.
     * Each int value is converted to a double value without loss of information.
     * The returned DoubleList is independent of this list.
     *
     * @return a new DoubleList containing all elements from this list converted to double values
     */
    public DoubleList toDoubleList() {
        final double[] a = new double[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i];//NOSONAR
        }

        return DoubleList.of(a);
    }

    /**
     * Returns a Collection containing the elements from the specified range converted to their boxed type.
     * The type of Collection returned is determined by the provided supplier function.
     * The returned collection is independent of this list.
     *
     * @param <C> the type of Collection to return
     * @param fromIndex the starting index (inclusive) of the range to convert
     * @param toIndex the ending index (exclusive) of the range to convert
     * @param supplier a function that creates a new Collection instance with the given initial capacity
     * @return a Collection containing Integer objects from the specified range
     * @throws IndexOutOfBoundsException if fromIndex < 0, toIndex > size(), or fromIndex > toIndex
     */
    @Override
    public <C extends Collection<Integer>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
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
     * @param fromIndex the starting index (inclusive) of the range to convert
     * @param toIndex the ending index (exclusive) of the range to convert
     * @param supplier a function that creates a new Multiset instance with the given initial capacity
     * @return a Multiset containing Integer objects from the specified range with their counts
     * @throws IndexOutOfBoundsException if fromIndex < 0, toIndex > size(), or fromIndex > toIndex
     */
    @Override
    public Multiset<Integer> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Integer>> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Integer> multiset = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            multiset.add(elementData[i]);
        }

        return multiset;
    }

    /**
     * Returns an iterator over all elements in this list.
     * The iterator returns elements in the order they appear in the list (from index 0 to size-1).
     * 
     * <p>The returned iterator does not support the remove operation.</p>
     *
     * @return an IntIterator over the elements in this list
     */
    @Override
    public IntIterator iterator() {
        if (isEmpty()) {
            return IntIterator.EMPTY;
        }

        return IntIterator.of(elementData, 0, size);
    }

    /**
     * Returns an IntStream with all elements of this list as its source.
     * The stream processes elements in the order they appear in the list.
     *
     * @return an IntStream of all elements in this list
     */
    public IntStream stream() {
        return IntStream.of(elementData, 0, size());
    }

    /**
     * Returns an IntStream with elements from the specified range of this list as its source.
     * The stream processes elements in the order they appear in the specified range.
     *
     * @param fromIndex the starting index (inclusive) of the range to stream
     * @param toIndex the ending index (exclusive) of the range to stream
     * @return an IntStream of elements in the specified range
     * @throws IndexOutOfBoundsException if fromIndex < 0, toIndex > size(), or fromIndex > toIndex
     */
    public IntStream stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return IntStream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in this list.
     * This method provides direct access without Optional wrapping.
     *
     * @return the first int value in the list
     * @throws NoSuchElementException if the list is empty
     */
    public int getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in this list.
     * This method provides direct access without Optional wrapping.
     *
     * @return the last int value in the list
     * @throws NoSuchElementException if the list is empty
     */
    public int getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list.
     * Shifts all existing elements to the right (adds one to their indices).
     * 
     * <p>This operation has O(n) time complexity where n is the size of the list.</p>
     *
     * @param e the element to add at the beginning of the list
     */
    public void addFirst(final int e) {
        add(0, e);
    }

    /**
     * Appends the specified element to the end of this list.
     * This operation has amortized O(1) time complexity.
     *
     * @param e the element to add at the end of the list
     */
    public void addLast(final int e) {
        add(size, e);
    }

    /**
     * Removes and returns the first element from this list.
     * Shifts all remaining elements to the left (subtracts one from their indices).
     * 
     * <p>This operation has O(n) time complexity where n is the size of the list.</p>
     *
     * @return the first int value that was removed from the list
     * @throws NoSuchElementException if the list is empty
     */
    public int removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(0);
    }

    /**
     * Removes and returns the last element from this list.
     * This operation has O(1) time complexity.
     *
     * @return the last int value that was removed from the list
     * @throws NoSuchElementException if the list is empty
     */
    public int removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(size - 1);
    }

    /**
     * Returns the hash code value for this list.
     * The hash code is computed based on the elements in the list and their order.
     * 
     * <p>The hash code is defined to be the result of the following calculation:
     * <pre>{@code
     * int hashCode = 1;
     * for (int e : list)
     *     hashCode = 31 * hashCode + e;
     * }</pre></p>
     *
     * @return the hash code value for this list
     */
    @Override
    public int hashCode() {
        return N.hashCode(elementData, 0, size);
    }

    /**
     * Compares the specified object with this list for equality.
     * Returns true if and only if the specified object is also an IntList,
     * both lists have the same size, and all corresponding pairs of elements
     * in the two lists are equal.
     * 
     * <p>Two int values are considered equal if they have the same value.</p>
     *
     * @param obj the object to be compared for equality with this list
     * @return true if the specified object is equal to this list, false otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof IntList other) {
            return size == other.size && N.equals(elementData, 0, other.elementData, 0, size);
        }

        return false;
    }

    /**
     * Returns a string representation of this list.
     * The string representation consists of the list's elements in order,
     * enclosed in square brackets ("[]"). Adjacent elements are separated
     * by the characters ", " (comma and space).
     * 
     * <p>Example: A list containing the integers 1, 2, and 3 would return "[1, 2, 3]".</p>
     *
     * @return a string representation of this list
     */
    @Override
    public String toString() {
        return size == 0 ? Strings.STR_FOR_EMPTY_ARRAY : N.toString(elementData, 0, size);
    }

    private void ensureCapacity(final int minCapacity) {
        if (minCapacity > MAX_ARRAY_SIZE || minCapacity < 0) {
            throw new OutOfMemoryError();
        }

        if (N.isEmpty(elementData)) {
            elementData = new int[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            final int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}

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
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.function.BooleanConsumer;
import com.landawn.abacus.util.function.BooleanPredicate;
import com.landawn.abacus.util.function.BooleanUnaryOperator;
import com.landawn.abacus.util.stream.Stream;

/**
 * A resizable array implementation for primitive boolean values. This class provides
 * methods to manipulate arrays of boolean primitives with operations similar to
 * {@code List<Boolean>} but with better performance and less memory overhead.
 * 
 * <p>The size, isEmpty, get, set operations run in constant time. The add operation
 * runs in amortized constant time. All other operations run in linear time or better.</p>
 * 
 * <p>Each BooleanList instance has a capacity. The capacity is the size of the array
 * used to store the elements in the list. It is always at least as large as the list
 * size. As elements are added to a BooleanList, its capacity grows automatically.</p>
 * 
 * <p>An application can increase the capacity of a BooleanList instance before adding
 * a large number of elements using the ensureCapacity operation. This may reduce the
 * amount of incremental reallocation.</p>
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 */
public final class BooleanList extends PrimitiveList<Boolean, boolean[], BooleanList> {

    @Serial
    private static final long serialVersionUID = -1194435277403867258L;

    static final Random RAND = new SecureRandom();

    private boolean[] elementData = N.EMPTY_BOOLEAN_ARRAY;

    private int size = 0;

    /**
     * Constructs an empty BooleanList with an initial capacity of zero.
     * The internal array will be allocated with a default capacity when the first element is added.
     */
    public BooleanList() {
    }

    /**
     * Constructs an empty BooleanList with the specified initial capacity.
     * 
     * <p>This constructor is useful when the approximate size of the list is known in advance,
     * as it can help avoid multiple array reallocations during element additions.</p>
     *
     * @param initialCapacity the initial capacity of the list. Must be non-negative.
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    public BooleanList(final int initialCapacity) {
        N.checkArgNotNegative(initialCapacity, cs.initialCapacity);

        elementData = initialCapacity == 0 ? N.EMPTY_BOOLEAN_ARRAY : new boolean[initialCapacity];
    }

    /**
     * Constructs a BooleanList using the specified array as the element array for this list without copying.
     * The list will have the same size as the array length.
     * 
     * <p><b>Note:</b> The array is used directly without copying. Any modifications to the list
     * will affect the original array and vice versa.</p>
     *
     * @param a the array to be used as the element array for this list. Must not be {@code null}.}
     */
    public BooleanList(final boolean[] a) {
        this(N.requireNonNull(a), a.length);
    }

    /**
     * Constructs a BooleanList using the specified array as the element array for this list without copying.
     * Only the first {@code size} elements of the array will be considered as part of the list.
     * 
     * <p><b>Note:</b> The array is used directly without copying. Any modifications to the list
     * will affect the original array and vice versa.</p>
     *
     * @param a the array to be used as the element array for this list. Must not be {@code null}.
     * @param size the number of elements in the list. Must be between 0 and the array length (inclusive).}
     * @throws IndexOutOfBoundsException if {@code size} is negative or greater than the array length
     */
    public BooleanList(final boolean[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates a new BooleanList containing the specified elements. The specified array is used directly
     * as the backing array without copying, so subsequent modifications to the array will affect the list.
     * If the input array is {@code null}, an empty list is returned.
     *
     * @param a the array of elements to be included in the new list. Can be {@code null}.
     * @return a new BooleanList containing the elements from the specified array, or an empty list if the array is {@code null}
     */
    public static BooleanList of(final boolean... a) {
        return new BooleanList(N.nullToEmpty(a));
    }

    /**
     * Creates a new BooleanList containing the first {@code size} elements of the specified array.
     * 
     * <p>If the input array is {@code null}, it is treated as an empty array. The {@code size}
     * parameter must be valid for the resulting array length.</p>
     *
     * @param a the array of boolean values to be included in the new list. Can be {@code null}.
     * @param size the number of elements from the array to include in the list.
     *             Must be between 0 and the array length (inclusive).
     * @return a new BooleanList containing the first {@code size} elements of the specified array
     * @throws IndexOutOfBoundsException if {@code size} is negative or greater than the array length
     */
    public static BooleanList of(final boolean[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new BooleanList(N.nullToEmpty(a), size);
    }

    /**
     * Creates a new BooleanList that is a copy of the specified array.
     * 
     * <p>Unlike {@link #of(boolean...)}, this method always creates a defensive copy of the input array,
     * ensuring that modifications to the returned list do not affect the original array.</p>
     * 
     * <p>If the input array is {@code null}, an empty list is returned.</p>
     *
     * @param a the array to be copied. Can be {@code null}.
     * @return a new BooleanList containing a copy of the elements from the specified array,
     *         or an empty list if the array is {@code null}
     */
    public static BooleanList copyOf(final boolean[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates a new BooleanList that is a copy of the specified range within the given array.
     * 
     * <p>This method creates a defensive copy of the elements in the specified range,
     * ensuring that modifications to the returned list do not affect the original array.</p>
     *
     * @param a the array from which a range is to be copied. Must not be {@code null}.
     * @param fromIndex the initial index of the range to be copied, inclusive.
     * @param toIndex the final index of the range to be copied, exclusive.
     * @return a new BooleanList containing a copy of the elements in the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > a.length}
     *                                   or {@code fromIndex > toIndex}}
     */
    public static BooleanList copyOf(final boolean[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates a new BooleanList with the specified element repeated a given number of times.
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list = BooleanList.repeat(true, 5); // [true, true, true, true, true]
     * }</pre></p>
     *
     * @param element the boolean value to be repeated
     * @param len the number of times to repeat the element. Must be non-negative.
     * @return a new BooleanList containing the repeated elements
     * @throws IllegalArgumentException if {@code len} is negative
     */
    public static BooleanList repeat(final boolean element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates a new BooleanList with random boolean values.
     * 
     * <p>Each element in the list has an equal probability of being {@code true} or {@code false}.
     * The random values are generated using a secure random number generator.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList randomList = BooleanList.random(10); // list with 10 random boolean values
     * }</pre></p>
     *
     * @param len the number of random boolean values to generate. Must be non-negative.
     * @return a new BooleanList containing random boolean values
     * @throws IllegalArgumentException if {@code len} is negative
     */
    public static BooleanList random(final int len) {
        final boolean[] a = new boolean[len];

        for (int i = 0; i < len; i++) {
            a[i] = RAND.nextBoolean();
        }

        return of(a);
    }

    /**
     * Returns the internal array backing this list without creating a copy.
     * 
     * <p><b>WARNING:</b> This method returns a direct reference to the internal array.
     * Any modifications to the returned array will affect this list and vice versa.
     * The returned array may be larger than the list size; only elements from index 0
     * to {@code size()-1} are valid list elements.</p>
     * 
     * <p>This method is marked as {@code @Beta} and should be used with caution.</p>
     *
     * @return the internal boolean array backing this list
     * @deprecated should call {@code toArray()}
     */
    @Beta
    @Deprecated
    @Override
    public boolean[] array() {
        return elementData;
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index the index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
     */
    public boolean get(final int index) { // NOSONAR
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
    public boolean set(final int index, final boolean e) {
        rangeCheck(index);

        final boolean oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     * Appends the specified element to the end of this list.
     * 
     * <p>This method runs in amortized constant time. If the internal array needs to be
     * resized to accommodate the new element, all existing elements will be copied to
     * a new, larger array.</p>
     *
     * @param e the element to be appended to this list
     */
    public void add(final boolean e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * 
     * <p>This method runs in linear time in the worst case (when inserting at the beginning
     * of the list), as it may need to shift all existing elements.</p>
     *
     * @param index the index at which the specified element is to be inserted
     * @param e the element to be inserted
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    public void add(final int index, final boolean e) {
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
     * Appends all elements from the specified BooleanList to the end of this list,
     * in the order that they appear in the specified list.
     * 
     * <p>This method runs in linear time with respect to the size of the specified list.</p>
     *
     * @param c the BooleanList containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call (i.e., if the
     *         specified list was not empty); {@code false} otherwise
     */
    @Override
    public boolean addAll(final BooleanList c) {
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
     * Inserts all elements from the specified BooleanList into this list, starting at
     * the specified position. Shifts the element currently at that position (if any)
     * and any subsequent elements to the right (increases their indices).
     * 
     * <p>The behavior is undefined if the specified list is modified during the operation.</p>
     *
     * @param index the index at which to insert the first element from the specified list
     * @param c the BooleanList containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call (i.e., if the
     *         specified list was not empty); {@code false} otherwise
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    @Override
    public boolean addAll(final int index, final BooleanList c) {
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
     * in the order that they appear in the array.
     * 
     * <p>This method is equivalent to {@code addAll(size(), a)}.</p>
     *
     * @param a the array containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call (i.e., if the
     *         array was not empty); {@code false} otherwise
     */
    @Override
    public boolean addAll(final boolean[] a) {
        return addAll(size(), a);
    }

    /**
     * Inserts all elements from the specified array into this list, starting at
     * the specified position. Shifts the element currently at that position (if any)
     * and any subsequent elements to the right (increases their indices).
     *
     * @param index the index at which to insert the first element from the specified array
     * @param a the array containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call (i.e., if the
     *         array was not empty); {@code false} otherwise
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index > size()})
     */
    @Override
    public boolean addAll(final int index, final boolean[] a) {
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

    private void rangeCheckForAdd(final int index) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    /**
     * Removes the first occurrence of the specified element from this list, if it is present.
     * If the list does not contain the element, it is unchanged.
     * 
     * <p>This method runs in linear time, as it may need to search through the entire list
     * to find the element.</p>
     *
     * @param e the element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element (and it was removed);
     *         {@code false} otherwise
     */
    public boolean remove(final boolean e) {
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
     * The list is compacted after removal, maintaining the relative order of remaining elements.
     * 
     * <p>This method runs in linear time and performs at most one pass through the list.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, false, true, false, true);
     * list.removeAllOccurrences(true); // list is now [false, false]
     * }</pre></p>
     *
     * @param e the element to be removed from this list
     * @return {@code true} if this list was modified (i.e., at least one element was removed);
     *         {@code false} otherwise
     */
    public boolean removeAllOccurrences(final boolean e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] != e) {
                elementData[w++] = elementData[i];
            }
        }

        final int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, false);

            size = w;
        }

        return numRemoved > 0;
    }

    /**
     *
     * @param index
     */
    private void fastRemove(final int index) {
        final int numMoved = size - index - 1;

        if (numMoved > 0) {
            N.copy(elementData, index + 1, elementData, index, numMoved);
        }

        elementData[--size] = false; // clear to let GC do its work
    }

    /**
     * Removes from this list all of its elements that are contained in the specified BooleanList.
     * 
     * <p>This method runs in quadratic time in the worst case, but uses a more efficient
     * set-based algorithm when the size conditions make it beneficial.</p>
     *
     * @param c the BooleanList containing elements to be removed from this list
     * @return {@code true} if this list was modified as a result of the call;
     *         {@code false} otherwise
     */
    @Override
    public boolean removeAll(final BooleanList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        return batchRemove(c, false) > 0;
    }

    /**
     * Removes from this list all of its elements that are contained in the specified array.
     * 
     * <p>This method is equivalent to {@code removeAll(BooleanList.of(a))}.</p>
     *
     * @param a the array containing elements to be removed from this list
     * @return {@code true} if this list was modified as a result of the call;
     *         {@code false} otherwise
     */
    @Override
    public boolean removeAll(final boolean[] a) {
        if (N.isEmpty(a)) {
            return false;
        }

        return removeAll(of(a));
    }

    /**
     * Removes all elements from this list that satisfy the given predicate.
     * 
     * <p>Elements are tested in order, and those for which the predicate returns {@code true}
     * are removed. The relative order of retained elements is preserved.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, false, true, false);
     * list.removeIf(b -> b); // removes all true values, list is now [false, false]
     * }</pre></p>
     *
     * @param p the predicate which returns {@code true} for elements to be removed
     * @return {@code true} if any elements were removed; {@code false} otherwise}
     */
    public boolean removeIf(final BooleanPredicate p) {
        final BooleanList tmp = new BooleanList(size());

        for (int i = 0; i < size; i++) {
            if (!p.test(elementData[i])) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, elementData, 0, tmp.size());
        N.fill(elementData, tmp.size(), size, false);
        size = tmp.size;

        return true;
    }

    /**
     * Removes duplicate elements from this list, keeping only distinct values.
     * 
     * <p>For boolean lists, this method will result in a list containing at most two elements:
     * one {@code true} and one {@code false}, preserving their first occurrence order.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, false, true, false, true);
     * list.removeDuplicates(); // list is now [true, false]
     * }</pre></p>
     *
     * @return {@code true} if the list was modified (i.e., duplicates were removed);
     *         {@code false} if the list already contained only distinct values
     */
    @Override
    public boolean removeDuplicates() {
        if (size < 2) {
            return false;
        } else if (size == 2) {
            if (elementData[0] == elementData[1]) {
                elementData[1] = false;
                size = 1;
                return true;
            } else {
                return false;
            }
        } else {
            for (int i = 1; i < size; i++) {
                if (elementData[i] != elementData[0]) {
                    elementData[1] = elementData[i];

                    N.fill(elementData, 2, size, false);
                    size = 2;
                    return true;
                }
            }

            N.fill(elementData, 1, size, false);
            size = 1;
            return true;
        }
    }

    /**
     * Retains only the elements in this list that are contained in the specified BooleanList.
     * In other words, removes from this list all of its elements that are not contained
     * in the specified list.
     * 
     * <p>This method runs in quadratic time in the worst case, but uses a more efficient
     * set-based algorithm when the size conditions make it beneficial.</p>
     *
     * @param c the BooleanList containing elements to be retained in this list
     * @return {@code true} if this list was modified as a result of the call;
     *         {@code false} otherwise
     */
    @Override
    public boolean retainAll(final BooleanList c) {
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
     * in the specified array.
     * 
     * <p>This method is equivalent to {@code retainAll(BooleanList.of(a))}.</p>
     *
     * @param a the array containing elements to be retained in this list
     * @return {@code true} if this list was modified as a result of the call;
     *         {@code false} otherwise
     */
    @Override
    public boolean retainAll(final boolean[] a) {
        if (N.isEmpty(a)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(BooleanList.of(a));
    }

    /**
     *
     * @param c
     * @param complement
     * @return
     */
    private int batchRemove(final BooleanList c, final boolean complement) {
        final boolean[] elementData = this.elementData;//NOSONAR

        int w = 0;

        if (c.size() > 3 && size() > 9) {
            final Set<Boolean> set = c.toSet();

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
            N.fill(elementData, w, size, false);

            size = w;
        }

        return numRemoved;
    }

    /**
     * Removes the element at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their indices).
     * 
     * <p>This method runs in linear time in the worst case, as it may need to shift
     * all elements after the removed element.</p>
     *
     * @param index the index of the element to be removed
     * @return the element that was removed from the list
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= size()})
     */
    public boolean delete(final int index) {
        rangeCheck(index);

        final boolean oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     * Removes the elements at the specified positions from this list.
     * The indices array will be sorted internally, and elements are removed in descending
     * order to maintain consistency.
     * 
     * <p>This method is more efficient than removing elements one by one when multiple
     * elements need to be removed.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, false, true, false, true);
     * list.deleteAllByIndices(1, 3); // removes elements at indices 1 and 3
     * // list is now [true, true, true]
     * }</pre></p>
     *
     * @param indices the indices of elements to be removed. Duplicate indices are allowed
     *                and will be handled appropriately.
     * @throws IndexOutOfBoundsException if any index is out of range
     *         ({@code index < 0 || index >= size()})
     */
    @Override
    public void deleteAllByIndices(final int... indices) {
        if (N.isEmpty(indices)) {
            return;
        }

        final boolean[] tmp = N.deleteAllByIndices(elementData, indices);

        N.copy(tmp, 0, elementData, 0, tmp.length);

        if (size > tmp.length) {
            N.fill(elementData, tmp.length, size, false);
        }

        size = size - (elementData.length - tmp.length);
    }

    /**
     * Removes a range of elements from this list.
     * The removal range is from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     * 
     * <p>Shifts any subsequent elements to the left (reduces their indices by
     * {@code toIndex - fromIndex}).</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, false, true, false, true);
     * list.deleteRange(1, 4); // removes elements at indices 1, 2, and 3
     * // list is now [true, true]
     * }</pre></p>
     *
     * @param fromIndex the index of the first element to be removed (inclusive)
     * @param toIndex the index after the last element to be removed (exclusive)
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *                                   or {@code fromIndex > toIndex}
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

        N.fill(elementData, newSize, size, false);

        this.size = newSize;
    }

    /**
     * Moves a range of elements within this list to a new position.
     * The elements from fromIndex (inclusive) to toIndex (exclusive) are moved
     * so that the element originally at fromIndex will be at newPositionAfterMove.
     * Other elements are shifted as necessary to accommodate the move.
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, false, true, false, true);
     * list.moveRange(1, 3, 2); // moves elements at indices 1-2 to start at index 2
     * // list is now [true, false, false, true, true]
     * }</pre></p>
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
     * Replaces a range of elements in this list with the elements from the specified BooleanList.
     * The range to be replaced is from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     * 
     * <p>If the replacement list has a different size than the range being replaced,
     * the list will be resized accordingly, and subsequent elements will be shifted.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, false, true, false, true);
     * BooleanList replacement = BooleanList.of(false, false);
     * list.replaceRange(1, 4, replacement);
     * // list is now [true, false, false, true]
     * }</pre></p>
     *
     * @param fromIndex the index of the first element to be replaced (inclusive)
     * @param toIndex the index after the last element to be replaced (exclusive)
     * @param replacement the BooleanList whose elements will replace the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *                                   or {@code fromIndex > toIndex}
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final BooleanList replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, false);
        }

        this.size = newSize;
    }

    /**
     * Replaces a range of elements in this list with the elements from the specified array.
     * The range to be replaced is from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     * 
     * <p>This method is equivalent to {@code replaceRange(fromIndex, toIndex, BooleanList.of(replacement))}.</p>
     *
     * @param fromIndex the index of the first element to be replaced (inclusive)
     * @param toIndex the index after the last element to be replaced (exclusive)
     * @param replacement the array whose elements will replace the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *                                   or {@code fromIndex > toIndex}
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final boolean[] replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, false);
        }

        this.size = newSize;
    }

    /**
     * Replaces all occurrences of the specified value with the new value in this list.
     * 
     * <p>This method runs in linear time, making a single pass through the list.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, false, true, false);
     * int count = list.replaceAll(true, false); // returns 2
     * // list is now [false, false, false, false]
     * }</pre></p>
     *
     * @param oldVal the value to be replaced
     * @param newVal the value to replace {@code oldVal}
     * @return the number of elements that were replaced
     */
    public int replaceAll(final boolean oldVal, final boolean newVal) {
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
     * Replaces each element of this list with the result of applying the operator to that element.
     * 
     * <p>This method runs in linear time, applying the operator to each element exactly once.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, false, true, false);
     * list.replaceAll(b -> !b); // negates all values
     * // list is now [false, true, false, true]
     * }</pre></p>
     *
     * @param operator the operator to apply to each element}
     */
    public void replaceAll(final BooleanUnaryOperator operator) {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsBoolean(elementData[i]);
        }
    }

    /**
     * Replaces all elements that satisfy the given predicate with the specified new value.
     * 
     * <p>This method runs in linear time, testing each element against the predicate
     * and replacing those that match.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, false, true, false);
     * boolean modified = list.replaceIf(b -> b, false); // replace all true with false
     * // list is now [false, false, false, false], modified is true
     * }</pre></p>
     *
     * @param predicate the predicate to test each element
     * @param newValue the value to replace matching elements with
     * @return {@code true} if at least one element was replaced; {@code false} otherwise}
     */
    public boolean replaceIf(final BooleanPredicate predicate, final boolean newValue) {
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
     * <p>This method is equivalent to {@code fill(0, size(), val)}.</p>
     *
     * @param val the value to be stored in all elements of the list
     */
    public void fill(final boolean val) {
        fill(0, size(), val);
    }

    /**
     * Replaces each element in the specified range of this list with the specified value.
     * The range to be filled extends from {@code fromIndex} (inclusive) to
     * {@code toIndex} (exclusive).
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, true, true, true, true);
     * list.fill(1, 4, false);
     * // list is now [true, false, false, false, true]
     * }</pre></p>
     *
     * @param fromIndex the index of the first element (inclusive) to be filled with the specified value
     * @param toIndex the index after the last element (exclusive) to be filled with the specified value
     * @param val the value to be stored in the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *                                   or {@code fromIndex > toIndex}
     */
    public void fill(final int fromIndex, final int toIndex, final boolean val) throws IndexOutOfBoundsException {
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
    public boolean contains(final boolean valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     * Returns {@code true} if this list contains any of the elements in the specified BooleanList.
     * 
     * <p>This method runs in O(n*m) time in the worst case, where n is the size of this list
     * and m is the size of the specified list. However, it may use more efficient algorithms
     * based on the relative sizes of the lists.</p>
     *
     * @param c the BooleanList to be checked for containment in this list
     * @return {@code true} if this list contains any of the elements in the specified list;
     *         {@code false} otherwise
     */
    @Override
    public boolean containsAny(final BooleanList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    /**
     * Returns {@code true} if this list contains any of the elements in the specified array.
     * 
     * <p>This method is equivalent to {@code containsAny(BooleanList.of(a))}.</p>
     *
     * @param a the array to be checked for containment in this list
     * @return {@code true} if this list contains any of the elements in the specified array;
     *         {@code false} otherwise
     */
    @Override
    public boolean containsAny(final boolean[] a) {
        if (isEmpty() || N.isEmpty(a)) {
            return false;
        }

        return !disjoint(a);
    }

    /**
     * Returns {@code true} if this list contains all of the elements in the specified BooleanList.
     * 
     * <p>This method runs in O(n*m) time in the worst case, where n is the size of this list
     * and m is the size of the specified list. However, it may use more efficient set-based
     * algorithms when beneficial.</p>
     *
     * @param c the BooleanList to be checked for containment in this list
     * @return {@code true} if this list contains all of the elements in the specified list;
     *         {@code false} otherwise
     */
    @Override
    public boolean containsAll(final BooleanList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        if (needToSet(size(), c.size())) {
            final Set<Boolean> set = this.toSet();

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
     * Returns {@code true} if this list contains all of the elements in the specified array.
     * 
     * <p>This method is equivalent to {@code containsAll(BooleanList.of(a))}.</p>
     *
     * @param a the array to be checked for containment in this list
     * @return {@code true} if this list contains all of the elements in the specified array;
     *         {@code false} otherwise
     */
    @Override
    public boolean containsAll(final boolean[] a) {
        if (N.isEmpty(a)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        return containsAll(of(a));
    }

    /**
     * Returns {@code true} if this list and the specified BooleanList have no elements in common.
     * 
     * <p>Two lists are disjoint if they share no common elements. Empty lists are disjoint
     * with all lists, including other empty lists.</p>
     *
     * @param c the BooleanList to be checked for disjointness with this list
     * @return {@code true} if this list and the specified list have no elements in common;
     *         {@code false} otherwise
     */
    @Override
    public boolean disjoint(final BooleanList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        if (needToSet(size(), c.size())) {
            final Set<Boolean> set = this.toSet();

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
     * 
     * <p>This method is equivalent to {@code disjoint(BooleanList.of(b))}.</p>
     *
     * @param b the array to be checked for disjointness with this list
     * @return {@code true} if this list and the specified array have no elements in common;
     *         {@code false} otherwise
     */
    @Override
    public boolean disjoint(final boolean[] b) {
        if (isEmpty() || N.isEmpty(b)) {
            return true;
        }

        return disjoint(of(b));
    }

    /**
     * Returns a new BooleanList containing all elements that appear in both this list and
     * the specified BooleanList, respecting the frequency of elements.
     * 
     * <p>The intersection preserves the order of elements from this list. If an element
     * appears multiple times in both lists, it will appear in the result the minimum
     * number of times it appears in either list.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list1 = BooleanList.of(true, false, true, false);
     * BooleanList list2 = BooleanList.of(true, true, false);
     * BooleanList result = list1.intersection(list2);
     * // result is [true, false, true]
     * }</pre></p>
     *
     * @param b the BooleanList to intersect with this list
     * @return a new BooleanList containing the intersection of this list and the specified list
     * @see IntList#intersection(IntList)
     */
    @Override
    public BooleanList intersection(final BooleanList b) {
        if (N.isEmpty(b)) {
            return new BooleanList();
        }

        final Multiset<Boolean> bOccurrences = b.toMultiset();

        final BooleanList c = new BooleanList(N.min(9, size(), b.size()));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Returns a new BooleanList containing all elements that appear in both this list and
     * the specified array, respecting the frequency of elements.
     * 
     * <p>This method is equivalent to {@code intersection(BooleanList.of(b))}.</p>
     *
     * @param b the array to intersect with this list
     * @return a new BooleanList containing the intersection of this list and the specified array
     * @see IntList#intersection(IntList)
     */
    @Override
    public BooleanList intersection(final boolean[] b) {
        if (N.isEmpty(b)) {
            return new BooleanList();
        }

        return intersection(of(b));
    }

    /**
     * Returns a new BooleanList containing the elements that are in this list but not in
     * the specified BooleanList, considering the frequency of elements.
     * 
     * <p>If an element appears n times in this list and m times in the specified list,
     * it will appear max(0, n-m) times in the result. The order of elements from this
     * list is preserved.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list1 = BooleanList.of(true, true, false, true);
     * BooleanList list2 = BooleanList.of(true, false);
     * BooleanList result = list1.difference(list2); // result is [true, true]
     * }</pre></p>
     *
     * @param b the BooleanList whose elements will be subtracted from this list
     * @return a new BooleanList containing the elements in this list but not in the specified list
     * @see #symmetricDifference(BooleanList)
     * @see N#difference(boolean[], boolean[])
     */
    @Override
    public BooleanList difference(final BooleanList b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Boolean> bOccurrences = b.toMultiset();

        final BooleanList c = new BooleanList(N.min(size(), N.max(9, size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (!bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Returns a new BooleanList containing the elements that are in this list but not in
     * the specified array, considering the frequency of elements.
     * 
     * <p>This method is equivalent to {@code difference(BooleanList.of(b))}.</p>
     *
     * @param b the array whose elements will be subtracted from this list
     * @return a new BooleanList containing the elements in this list but not in the specified array
     * @see #difference(BooleanList)
     * @see N#difference(boolean[], boolean[])
     */
    @Override
    public BooleanList difference(final boolean[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(b));
    }

    /**
     * Returns a new BooleanList containing the symmetric difference between this list and
     * the specified BooleanList. The symmetric difference consists of elements that are
     * in either list but not in both, considering the frequency of elements.
     * 
     * <p>The result contains all elements that appear in exactly one of the two lists,
     * or elements that appear a different number of times in each list.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list1 = BooleanList.of(true, true, false);
     * BooleanList list2 = BooleanList.of(true, false, false);
     * BooleanList result = list1.symmetricDifference(list2);
     * // result is [true, false] (one extra true from list1, one extra false from list2)
     * }</pre></p>
     *
     * @param b the BooleanList to compute symmetric difference with
     * @return a new BooleanList containing the symmetric difference of the two lists
     * @see IntList#symmetricDifference(IntList)
     */
    @Override
    public BooleanList symmetricDifference(final BooleanList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (isEmpty()) {
            return b.copy();
        }

        final Multiset<Boolean> bOccurrences = b.toMultiset();
        final BooleanList c = new BooleanList(N.max(9, Math.abs(size() - b.size())));

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
     * Returns a new BooleanList containing the symmetric difference between this list and
     * the specified array. The symmetric difference consists of elements that are
     * in either the list or array but not in both, considering the frequency of elements.
     * 
     * <p>This method is equivalent to {@code symmetricDifference(BooleanList.of(b))}.</p>
     *
     * @param b the array to compute symmetric difference with
     * @return a new BooleanList containing the symmetric difference
     * @see IntList#symmetricDifference(IntList)
     */
    @Override
    public BooleanList symmetricDifference(final boolean[] b) {
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
     * <p>This method runs in linear time, examining each element once.</p>
     * 
     * <p>Example usage:
     * <pre>{@code
     * BooleanList list = BooleanList.of(true, false, true, false, true);
     * int count = list.occurrencesOf(true); // returns 3
     * }</pre></p>
     *
     * @param valueToFind the value whose frequency is to be determined
     * @return the number of times the specified value appears in this list
     */
    public int occurrencesOf(final boolean valueToFind) {
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
     * or {@code -1} if this list does not contain the element.
     * 
     * <p>This method runs in linear time.</p>
     *
     * @param valueToFind the element to search for
     * @return the index of the first occurrence of the specified element in this list,
     *         or {@code -1} if this list does not contain the element
     */
    public int indexOf(final boolean valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * searching forwards from the specified index, or {@code -1} if the element is not found.
     * 
     * <p>This method runs in linear time relative to the distance between {@code fromIndex}
     * and the end of the list.</p>
     *
     * @param valueToFind the element to search for
     * @param fromIndex the index to start searching from (inclusive). If negative, search
     *                  starts from index 0. If greater than or equal to the list size,
     *                  returns {@code -1}.
     * @return the index of the first occurrence of the specified element at or after
     *         {@code fromIndex}, or {@code -1} if the element is not found
     */
    public int indexOf(final boolean valueToFind, final int fromIndex) {
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
     * or {@code -1} if this list does not contain the element.
     * 
     * <p>This method runs in linear time, searching backwards from the end of the list.</p>
     *
     * @param valueToFind the element to search for
     * @return the index of the last occurrence of the specified element in this list,
     *         or {@code -1} if this list does not contain the element
     */
    public int lastIndexOf(final boolean valueToFind) {
        return lastIndexOf(valueToFind, size - 1);
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * searching backwards from the specified index, or {@code -1} if the element is not found.
     * 
     * <p>The search begins at {@code startIndexFromBack} (inclusive) and proceeds backwards
     * towards index 0.</p>
     *
     * @param valueToFind the element to search for
     * @param startIndexFromBack the index to start searching backwards from (inclusive).
     *                          If greater than or equal to the list size, search starts
     *                          from the last element. If negative, returns {@code -1}.
     * @return the index of the last occurrence of the specified element at or before
     *         {@code startIndexFromBack}, or {@code -1} if the element is not found
     */
    public int lastIndexOf(final boolean valueToFind, final int startIndexFromBack) {
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
     * Performs the given action for each element in this list in sequential order.
     * The action is performed on each element from index 0 to size-1.
     *
     * @param action the action to be performed for each element. Must not be null.
     */
    public void forEach(final BooleanConsumer action) {
        forEach(0, size, action);
    }

    /**
     * Performs the given action for each element in the specified range of this list.
     * The action is performed on elements from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     * 
     * <p>If {@code fromIndex} is greater than {@code toIndex}, the elements are processed in reverse order,
     * from {@code fromIndex} (inclusive) down to {@code toIndex} (exclusive). If {@code toIndex} is -1,
     * it is treated as 0 when {@code fromIndex > toIndex}.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to process
     * @param toIndex the ending index (exclusive) of the range to process, or -1 to process from fromIndex to 0
     * @param action the action to be performed for each element. Must not be null.
     * @throws IndexOutOfBoundsException if {@code fromIndex} or {@code toIndex} is out of range
     *         ({@code fromIndex < 0 || toIndex > size() || (fromIndex > toIndex && toIndex != -1)})
     */
    public void forEach(final int fromIndex, final int toIndex, final BooleanConsumer action) throws IndexOutOfBoundsException {
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
     * Returns an {@code OptionalBoolean} containing the first element of this list,
     * or an empty {@code OptionalBoolean} if this list is empty.
     *
     * @return an {@code OptionalBoolean} containing the first element, or empty if the list is empty
     */
    public OptionalBoolean first() {
        return size() == 0 ? OptionalBoolean.empty() : OptionalBoolean.of(elementData[0]);
    }

    /**
     * Returns an {@code OptionalBoolean} containing the last element of this list,
     * or an empty {@code OptionalBoolean} if this list is empty.
     *
     * @return an {@code OptionalBoolean} containing the last element, or empty if the list is empty
     */
    public OptionalBoolean last() {
        return size() == 0 ? OptionalBoolean.empty() : OptionalBoolean.of(elementData[size() - 1]);
    }

    /**
     * Returns a new {@code BooleanList} containing the distinct elements from the specified range
     * of this list. For boolean values, this means the result will contain at most two elements
     * (true and false), preserving the order of their first occurrence in the original range.
     *
     * @param fromIndex the starting index (inclusive) of the range to process
     * @param toIndex the ending index (exclusive) of the range to process
     * @return a new {@code BooleanList} containing the distinct elements from the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public BooleanList distinct(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            return of(N.distinct(elementData, fromIndex, toIndex));
        } else {
            return of(N.copyOfRange(elementData, fromIndex, toIndex));
        }
    }

    /**
     * Checks whether this list contains duplicate elements. For a boolean list,
     * duplicates exist if the list contains at least two elements with the same value
     * (either two or more true values, or two or more false values).
     *
     * @return {@code true} if this list contains duplicate elements, {@code false} otherwise.
     *         An empty list or a list with one element returns {@code false}.
     *         A list with exactly two different boolean values (one true and one false) returns {@code false}.
     */
    @Override
    public boolean hasDuplicates() {
        if (size < 2) {
            return false;
        } else if (size == 2) {
            return elementData[0] == elementData[1];
        } else {
            return true;
        }
    }

    /**
     * Checks whether the elements in this list are sorted in ascending order.
     * For boolean values, ascending order means false values come before true values.
     * An empty list or a list with a single element is considered sorted.
     *
     * @return {@code true} if all elements are sorted in ascending order (false before true),
     *         {@code false} otherwise
     */
    @Override
    public boolean isSorted() {
        return N.isSorted(elementData, 0, size);
    }

    /**
     * Sorts this list in ascending order. For boolean values, this means all false values
     * will be placed before all true values. The relative order of equal elements is not preserved.
     * 
     * <p>This operation modifies the list in-place and has O(n) time complexity.</p>
     */
    @Override
    public void sort() {
        if (size <= 1) {
            return;
        }

        final int[] count = new int[2];

        for (int i = 0; i < size; i++) {
            count[!elementData[i] ? 0 : 1]++;
        }

        if (count[0] > 0) {
            N.fill(elementData, 0, count[0], false);
        }

        if (count[1] > 0) {
            N.fill(elementData, count[0], count[0] + count[1], true);
        }
    }

    /**
     * Sorts this list in descending order. For boolean values, this means all true values
     * will be placed before all false values. This is equivalent to calling {@link #sort()}
     * followed by {@link #reverse()}.
     * 
     * <p>This operation modifies the list in-place and has O(n) time complexity.</p>
     */
    @Override
    public void reverseSort() {
        if (size > 1) {
            sort();
            reverse();
        }
    }

    /**
     * Reverses the order of all elements in this list. The first element becomes the last,
     * the second becomes second to last, and so on.
     * 
     * <p>This operation modifies the list in-place and has O(n/2) time complexity.</p>
     */
    @Override
    public void reverse() {
        if (size > 1) {
            N.reverse(elementData, 0, size);
        }
    }

    /**
     * Reverses the order of elements in the specified range of this list.
     * Elements outside the specified range are not affected.
     *
     * @param fromIndex the starting index (inclusive) of the range to reverse
     * @param toIndex the ending index (exclusive) of the range to reverse
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
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
     * Randomly shuffles the elements in this list using the default random number generator.
     * Each possible permutation occurs with approximately equal probability.
     * 
     * <p>This operation modifies the list in-place and has O(n) time complexity.</p>
     */
    @Override
    public void shuffle() {
        if (size() > 1) {
            N.shuffle(elementData, 0, size);
        }
    }

    /**
     * Randomly shuffles the elements in this list using the specified random number generator.
     * Each possible permutation occurs with approximately equal probability, assuming that
     * the source of randomness is fair.
     *
     * @param rnd the random number generator to use for shuffling. Must not be null.
     */
    @Override
    public void shuffle(final Random rnd) {
        if (size() > 1) {
            N.shuffle(elementData, 0, size, rnd);
        }
    }

    /**
     * Swaps the elements at the specified positions in this list. If the specified positions
     * are the same, invoking this method leaves the list unchanged.
     *
     * @param i the index of the first element to swap
     * @param j the index of the second element to swap
     * @throws IndexOutOfBoundsException if either {@code i} or {@code j} is out of range
     *         ({@code i < 0 || i >= size() || j < 0 || j >= size()})
     */
    @Override
    public void swap(final int i, final int j) {
        rangeCheck(i);
        rangeCheck(j);

        set(i, set(j, elementData[i]));
    }

    /**
     * Returns a new {@code BooleanList} containing all elements of this list.
     * The returned list is independent of this list, so changes to the returned
     * list will not affect this list and vice versa.
     *
     * @return a new {@code BooleanList} containing all elements of this list
     */
    @Override
    public BooleanList copy() {
        return new BooleanList(N.copyOfRange(elementData, 0, size));
    }

    /**
     * Returns a new {@code BooleanList} containing the elements in the specified range
     * of this list. The returned list is independent of this list.
     *
     * @param fromIndex the starting index (inclusive) of the range to copy
     * @param toIndex the ending index (exclusive) of the range to copy
     * @return a new {@code BooleanList} containing the elements in the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public BooleanList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new BooleanList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     * Returns a new {@code BooleanList} containing elements from the specified range of this list,
     * sampled at regular intervals defined by the step parameter.
     * 
     * <p>If {@code fromIndex < toIndex} and {@code step > 0}, elements are sampled from low to high indices.
     * If {@code fromIndex > toIndex} and {@code step < 0}, elements are sampled from high to low indices.
     * The first element sampled is at {@code fromIndex}, then {@code fromIndex + step}, and so on.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to copy
     * @param toIndex the ending index (exclusive) of the range to copy. May be -1 when fromIndex > toIndex
     * @param step the sampling interval. Must not be zero. Positive for forward sampling,
     *             negative for backward sampling
     * @return a new {@code BooleanList} containing the sampled elements
     * @throws IndexOutOfBoundsException if the indices are out of range
     * @throws IllegalArgumentException if {@code step} is zero
     * @see N#copyOfRange(int[], int, int, int)
     */
    @Override
    public BooleanList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex));

        return new BooleanList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Splits this list into consecutive chunks of the specified size and returns them as a
     * list of {@code BooleanList} objects. The final chunk may be smaller than the specified
     * size if the range doesn't divide evenly.
     * 
     * <p>For example, splitting a list of 10 elements with chunk size 3 will produce
     * chunks of sizes [3, 3, 3, 1].</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to split
     * @param toIndex the ending index (exclusive) of the range to split
     * @param chunkSize the desired size of each chunk. Must be greater than 0.
     * @return a list of {@code BooleanList} objects, each containing a chunk of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     * @throws IllegalArgumentException if {@code chunkSize <= 0}
     */
    @Override
    public List<BooleanList> split(final int fromIndex, final int toIndex, final int chunkSize) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<boolean[]> list = N.split(elementData, fromIndex, toIndex, chunkSize);
        @SuppressWarnings("rawtypes")
        final List<BooleanList> result = (List) list;

        for (int i = 0, len = list.size(); i < len; i++) {
            result.set(i, of(list.get(i)));
        }

        return result;
    }

    /**
     * Trims the capacity of this list to be the list's current size. This operation
     * minimizes the memory footprint of the list by removing any unused capacity.
     * 
     * <p>If the capacity is already equal to the size, this method does nothing.
     * This method can be used to minimize the storage of a {@code BooleanList} instance.</p>
     *
     * @return this list instance
     */
    @Override
    public BooleanList trimToSize() {
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
            N.fill(elementData, 0, size, false);
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
     * Returns a {@code List<Boolean>} containing all elements of this list.
     * Each primitive boolean value is boxed into a {@code Boolean} object.
     * 
     * <p>The returned list is independent of this list, so changes to the returned
     * list will not affect this list and vice versa.</p>
     *
     * @return a new {@code List<Boolean>} containing all elements of this list as boxed values
     */
    @Override
    public List<Boolean> boxed() {
        return boxed(0, size);
    }

    /**
     * Returns a {@code List<Boolean>} containing the elements in the specified range
     * of this list. Each primitive boolean value is boxed into a {@code Boolean} object.
     * 
     * <p>The returned list is independent of this list, so changes to the returned
     * list will not affect this list and vice versa.</p>
     *
     * @param fromIndex the starting index (inclusive) of the range to box
     * @param toIndex the ending index (exclusive) of the range to box
     * @return a new {@code List<Boolean>} containing the specified range of elements as boxed values
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public List<Boolean> boxed(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Boolean> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    /**
     * Returns a new array containing all elements of this list in proper sequence.
     *
     * @return a new boolean array containing all elements of this list
     */
    @Override
    public boolean[] toArray() {
        return N.copyOfRange(elementData, 0, size);
    }

    /**
     * Returns a Collection containing the elements from the specified range converted to their boxed type.
     * The type of Collection returned is determined by the provided supplier function.
     * The returned collection is independent of this list.
     *
     * @param <C> the type of Collection to return
     * @param fromIndex the starting index (inclusive) of the range to convert
     * @param toIndex the ending index (exclusive) of the range to convert
     * @param supplier a function that creates a new Collection instance with the specified
     *                 initial capacity. The function receives the number of elements as input.
     * @return a Collection containing the boxed boolean values from the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public <C extends Collection<Boolean>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
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
     * @param supplier a function that creates a new Multiset instance with the specified
     *                 initial capacity. The function receives the number of elements as input.
     * @return a Multiset containing the boxed boolean values from the specified range with their counts
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public Multiset<Boolean> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Boolean>> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Boolean> multiset = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            multiset.add(elementData[i]);
        }

        return multiset;
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     * The iterator does not support the {@code remove} operation.
     *
     * @return a {@code BooleanIterator} over the elements in this list
     */
    @Override
    public BooleanIterator iterator() {
        if (isEmpty()) {
            return BooleanIterator.EMPTY;
        }

        return BooleanIterator.of(elementData, 0, size);
    }

    /**
     * Returns a {@code Stream} with this list as its source.
     * The stream will process all elements of this list in order.
     *
     * @return a sequential {@code Stream} over the elements in this list
     */
    public Stream<Boolean> stream() {
        return Stream.of(elementData, 0, size());
    }

    /**
     * Returns a {@code Stream} with the specified range of this list as its source.
     * The stream will process elements from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     *
     * @param fromIndex the starting index (inclusive) of the range to stream
     * @param toIndex the ending index (exclusive) of the range to stream
     * @return a sequential {@code Stream} over the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    public Stream<Boolean> stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return Stream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in this list.
     * 
     * <p>This method provides direct access to the first element without creating an Optional.</p>
     *
     * @return the first boolean value in the list
     * @throws NoSuchElementException if the list is empty
     */
    public boolean getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in this list.
     * 
     * <p>This method provides direct access to the last element without creating an Optional.</p>
     *
     * @return the last boolean value in the list
     * @throws NoSuchElementException if the list is empty
     */
    public boolean getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list. All existing elements
     * are shifted to the right (their indices are incremented by one).
     * 
     * <p>This operation has O(n) time complexity due to the need to shift existing elements.</p>
     *
     * @param e the boolean value to insert at the beginning of the list
     */
    public void addFirst(final boolean e) {
        add(0, e);
    }

    /**
     * Appends the specified element to the end of this list.
     * 
     * <p>This operation has amortized O(1) time complexity.</p>
     *
     * @param e the boolean value to append to the list
     */
    public void addLast(final boolean e) {
        add(size, e);
    }

    /**
     * Removes and returns the first element from this list. All remaining elements
     * are shifted to the left (their indices are decremented by one).
     * 
     * <p>This operation has O(n) time complexity due to the need to shift remaining elements.</p>
     *
     * @return the first boolean value that was removed from the list
     * @throws NoSuchElementException if the list is empty
     */
    public boolean removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(0);
    }

    /**
     * Removes and returns the last element from this list.
     * 
     * <p>This operation has O(1) time complexity.</p>
     *
     * @return the last boolean value that was removed from the list
     * @throws NoSuchElementException if the list is empty
     */
    public boolean removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(size - 1);
    }

    /**
     * Returns a hash code value for this list. The hash code is calculated based on
     * the elements in the list and their order.
     * 
     * <p>The hash code is defined to be the result of the following calculation:
     * <pre>{@code
     * int hashCode = 1;
     * for (boolean e : list)
     *     hashCode = 31 * hashCode + (e ? 1231 : 1237);
     * }</pre></p>
     *
     * @return the hash code value for this list
     */
    @Override
    public int hashCode() {
        return N.hashCode(elementData, 0, size);
    }

    /**
     * Compares the specified object with this list for equality. Returns {@code true}
     * if and only if the specified object is also a {@code BooleanList}, both lists
     * have the same size, and all corresponding pairs of elements in the two lists
     * are equal.
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

        if (obj instanceof BooleanList other) {
            return size == other.size && N.equals(elementData, 0, other.elementData, 0, size);
        }

        return false;
    }

    /**
     * Returns a string representation of this list. The string representation consists
     * of the list's elements in order, enclosed in square brackets ("[]"). Adjacent
     * elements are separated by the characters ", " (comma and space).
     * 
     * <p>Example: A list containing true, false, true would return "[true, false, true]"</p>
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
            elementData = new boolean[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            final int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}

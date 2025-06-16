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
 * A resizable array implementation for primitive int values.
 * This class provides a more memory-efficient alternative to {@code ArrayList<Integer>}
 * by storing primitive int values directly, avoiding the overhead of boxing/unboxing.
 *
 * <p>This class is not thread-safe. If multiple threads access an IntList instance concurrently,
 * and at least one of the threads modifies the list structurally, it must be synchronized externally.
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
     * Constructs an empty IntList with default initial capacity.
     */
    public IntList() {
    }

    /**
     * Constructs an empty IntList with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    public IntList(final int initialCapacity) {
        elementData = initialCapacity == 0 ? N.EMPTY_INT_ARRAY : new int[initialCapacity];
    }

    /**
     * Constructs an IntList containing the elements of the specified array.
     * The list will have the same length as the array, and will contain the same elements in the same order.
     *
     * @param a the array whose elements are to be placed into this list
     * @throws NullPointerException if the specified array is null
     */
    public IntList(final int[] a) {
        this(N.requireNonNull(a), a.length);
    }

    /**
     * Constructs an IntList using the specified array as the backing array with the specified size.
     * The list will use the provided array directly without copying.
     *
     * @param a the array to be used as the backing array for this list
     * @param size the number of valid elements in the array
     * @throws NullPointerException if the specified array is null
     * @throws IndexOutOfBoundsException if size is negative or greater than the array length
     */
    public IntList(final int[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates a new IntList containing the specified elements.
     *
     * @param a the elements to be contained in the new list
     * @return a new IntList containing the specified elements
     */
    public static IntList of(final int... a) {
        return new IntList(N.nullToEmpty(a));
    }

    /**
     * Creates a new IntList containing the specified number of elements from the given array.
     *
     * @param a the array containing elements to be placed in the new list
     * @param size the number of elements from the array to include in the list
     * @return a new IntList containing the specified elements
     * @throws IndexOutOfBoundsException if size is negative or greater than the array length
     */
    public static IntList of(final int[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new IntList(N.nullToEmpty(a), size);
    }

    /**
     * Creates a new IntList that is a copy of the specified array.
     * The returned list will contain the same elements as the array but will have its own backing array.
     *
     * @param a the array to copy
     * @return a new IntList containing a copy of the specified array's elements
     */
    public static IntList copyOf(final int[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates a new IntList that is a copy of the specified range of the array.
     *
     * @param a the array from which a range is to be copied
     * @param fromIndex the initial index of the range to be copied, inclusive
     * @param toIndex the final index of the range to be copied, exclusive
     * @return a new IntList containing the specified range of the array
     * @throws IndexOutOfBoundsException if fromIndex < 0 or toIndex > a.length or fromIndex > toIndex
     */
    public static IntList copyOf(final int[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates a new IntList containing a sequence of integers from startInclusive (inclusive) to endExclusive (exclusive).
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a new IntList containing the sequence of integers
     */
    public static IntList range(final int startInclusive, final int endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     * Creates a new IntList containing a sequence of integers from startInclusive (inclusive) to endExclusive (exclusive)
     * with the specified step.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param by the step value
     * @return a new IntList containing the sequence of integers
     * @throws IllegalArgumentException if by is zero
     */
    public static IntList range(final int startInclusive, final int endExclusive, final int by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     * Creates a new IntList containing a sequence of integers from startInclusive to endInclusive (both inclusive).
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a new IntList containing the sequence of integers
     */
    public static IntList rangeClosed(final int startInclusive, final int endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     * Creates a new IntList containing a sequence of integers from startInclusive to endInclusive (both inclusive)
     * with the specified step.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @param by the step value
     * @return a new IntList containing the sequence of integers
     * @throws IllegalArgumentException if by is zero
     */
    public static IntList rangeClosed(final int startInclusive, final int endInclusive, final int by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     * Creates a new IntList filled with the specified element repeated the specified number of times.
     *
     * @param element the element to repeat
     * @param len the number of times to repeat the element
     * @return a new IntList containing the repeated element
     * @throws IllegalArgumentException if len is negative
     */
    public static IntList repeat(final int element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates a new IntList filled with random integer values.
     *
     * @param len the number of random values to generate
     * @return a new IntList containing random integer values
     * @throws IllegalArgumentException if len is negative
     * @see Random#nextInt()
     */
    public static IntList random(final int len) {
        final int[] a = new int[len];

        for (int i = 0; i < len; i++) {
            a[i] = RAND.nextInt();
        }

        return of(a);
    }

    /**
     * Creates a new IntList filled with random integer values within the specified range.
     *
     * @param startInclusive the lower bound (inclusive) of the random values
     * @param endExclusive the upper bound (exclusive) of the random values
     * @param len the number of random values to generate
     * @return a new IntList containing random integer values within the specified range
     * @throws IllegalArgumentException if startInclusive >= endExclusive or len is negative
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
     * Returns the underlying int array backing this list.
     * Changes to the returned array will directly affect this list.
     *
     * @return the underlying int array
     */
    @Beta
    @Override
    public int[] array() {
        return elementData;
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index the index of the element to return
     * @return the element at the specified position
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size())
     */
    public int get(final int index) {
        rangeCheck(index);

        return elementData[index];
    }

    /**
     *
     * @param index
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
     *
     * @param e the element to be appended to this list
     */
    public void add(final int e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any subsequent elements to the right.
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
     * Appends all elements in the specified IntList to the end of this list.
     *
     * @param c the IntList containing elements to be added to this list
     * @return true if this list changed as a result of the call
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
     * Inserts all elements in the specified IntList into this list at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to the right.
     *
     * @param index the index at which to insert the first element from the specified IntList
     * @param c the IntList containing elements to be added to this list
     * @return true if this list changed as a result of the call
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
     * Appends all elements in the specified array to the end of this list.
     *
     * @param a the array containing elements to be added to this list
     * @return true if this list changed as a result of the call
     */
    @Override
    public boolean addAll(final int[] a) {
        return addAll(size(), a);
    }

    /**
     * Inserts all elements in the specified array into this list at the specified position.
     * Shifts the element currently at that position (if any) and any subsequent elements to the right.
     *
     * @param index the index at which to insert the first element from the specified array
     * @param a the array containing elements to be added to this list
     * @return true if this list changed as a result of the call
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

    private void rangeCheckForAdd(final int index) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    /**
     * Removes the first occurrence of the specified element from this list, if it is present.
     * Shifts any subsequent elements to the left.
     *
     * @param e the element to be removed from this list, if present
     * @return true if this list contained the specified element
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
     * Shifts any subsequent elements to the left.
     *
     * @param e the element to be removed from this list
     * @return true if this list was modified as a result of this operation
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
     *
     * @param index
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
     *
     * @param c the IntList containing elements to be removed from this list
     * @return true if this list changed as a result of the call
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
     *
     * @param a the array containing elements to be removed from this list
     * @return true if this list changed as a result of the call
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
     *
     * @param p the predicate which returns true for elements to be removed
     * @return true if any elements were removed
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
     * Removes duplicate elements from this list, preserving the order of first occurrences.
     *
     * @return true if any duplicates were removed
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
     *
     * @param c the IntList containing elements to be retained in this list
     * @return true if this list changed as a result of the call
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
     *
     * @param a the array containing elements to be retained in this list
     * @return true if this list changed as a result of the call
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
     *
     * @param c
     * @param complement
     * @return
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
     * Removes the element at the specified position in this list.
     * Shifts any subsequent elements to the left.
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
     * The indices array will be sorted internally and duplicates will be ignored.
     *
     * @param indices the indices of elements to be removed
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
     * Removes from this list all elements whose index is between fromIndex (inclusive) and toIndex (exclusive).
     *
     * @param fromIndex the index of the first element to be removed
     * @param toIndex the index after the last element to be removed
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
     * The elements in the range [fromIndex, toIndex) will be moved to start at newPositionStartIndexAfterMove.
     *
     * @param fromIndex the starting index (inclusive) of the range to be moved
     * @param toIndex the ending index (exclusive) of the range to be moved
     * @param newPositionStartIndexAfterMove the starting index where the range will be placed after the move
     * @throws IndexOutOfBoundsException if any index is out of bounds or if the new position would cause elements to be moved outside the list
     */
    @Override
    public void moveRange(final int fromIndex, final int toIndex, final int newPositionStartIndexAfterMove) {
        N.moveRange(elementData, fromIndex, toIndex, newPositionStartIndexAfterMove);
    }

    /**
     * Replaces a range of elements in this list with elements from the specified IntList.
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
     * Replaces a range of elements in this list with elements from the specified array.
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
     * Replaces all occurrences of the specified value with the new value.
     *
     * @param oldVal the value to be replaced
     * @param newVal the value to replace oldVal
     * @return the number of elements replaced
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
     * Replaces each element of this list with the result of applying the operator to that element.
     *
     * @param operator the operator to apply to each element
     */
    public void replaceAll(final IntUnaryOperator operator) {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsInt(elementData[i]);
        }
    }

    /**
     * Replaces all elements that satisfy the given predicate with the specified value.
     *
     * @param predicate the predicate to test each element
     * @param newValue the value to replace matching elements
     * @return true if any elements were replaced
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
     * Replaces elements in the specified range with the specified value.
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @param val the value to be stored in the specified range
     * @throws IndexOutOfBoundsException if fromIndex < 0 or toIndex > size() or fromIndex > toIndex
     */
    public void fill(final int fromIndex, final int toIndex, final int val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     * Returns true if this list contains the specified element.
     *
     * @param valueToFind the element whose presence in this list is to be tested
     * @return true if this list contains the specified element
     */
    public boolean contains(final int valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     * Returns true if this list contains any of the elements in the specified IntList.
     *
     * @param c the IntList to be checked for containment in this list
     * @return true if this list contains any element from the specified IntList
     */
    @Override
    public boolean containsAny(final IntList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    /**
     * Returns true if this list contains any of the elements in the specified array.
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
     * Returns true if this list contains all of the elements in the specified IntList.
     *
     * @param c the IntList to be checked for containment in this list
     * @return true if this list contains all elements of the specified IntList
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
     * Returns true if this list contains all of the elements in the specified array.
     *
     * @param a the array to be checked for containment in this list
     * @return true if this list contains all elements of the specified array
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
     *
     * @param c the IntList to check for common elements
     * @return true if this list has no elements in common with the specified IntList
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
     *
     * @param b the array to check for common elements
     * @return true if this list has no elements in common with the specified array
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
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences present in both lists.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * IntList list2 = IntList.of(1, 2, 2, 4);
     * IntList result = list1.intersection(list2); // result will be [1, 2, 2]
     * // One occurrence of '1' (minimum count in both lists) and two occurrences of '2'
     *
     * IntList list3 = IntList.of(5, 5, 6);
     * IntList list4 = IntList.of(5, 7);
     * IntList result2 = list3.intersection(list4); // result will be [5]
     * // One occurrence of '5' (minimum count in both lists)
     * </pre>
     *
     * @param b the list to find common elements with this list
     * @return a new IntList containing elements present in both this list and the specified list,
     *         considering the minimum number of occurrences in either list.
     *         Returns an empty list if either list is {@code null} or empty.
     * @see #intersection(int[])
     * @see #difference(IntList)
     * @see #symmetricDifference(IntList)
     * @see N#intersection(int[], int[])
     * @see N#intersection(Collection, Collection)
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
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences present in both sources.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * int[] array = new int[]{1, 2, 2, 4};
     * IntList result = list1.intersection(array); // result will be [1, 2, 2]
     * // One occurrence of '1' (minimum count in both sources) and two occurrences of '2'
     *
     * IntList list2 = IntList.of(5, 5, 6);
     * int[] array2 = new int[]{5, 7};
     * IntList result2 = list2.intersection(array2); // result will be [5]
     * // One occurrence of '5' (minimum count in both sources)
     * </pre>
     *
     * @param b the array to find common elements with this list
     * @return a new IntList containing elements present in both this list and the specified array,
     *         considering the minimum number of occurrences in either source.
     *         Returns an empty list if the array is {@code null} or empty.
     * @see #intersection(IntList)
     * @see #difference(int[])
     * @see #symmetricDifference(int[])
     * @see N#intersection(int[], int[])
     * @see N#intersection(Collection, Collection)
     */
    @Override
    public IntList intersection(final int[] b) {
        if (N.isEmpty(b)) {
            return new IntList();
        }

        return intersection(of(b));
    }

    /**
     * Returns a new list with the elements in this list but not in the specified list {@code b},
     * considering the number of occurrences of each element.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * IntList list2 = IntList.of(2, 5, 1);
     * IntList result = list1.difference(list2); // result will be [0, 2, 3]
     * // One '2' remains because list1 has two occurrences and list2 has one
     *
     * IntList list3 = IntList.of(5, 6);
     * IntList list4 = IntList.of(5, 5, 6);
     * IntList result2 = list3.difference(list4); // result will be [] (empty)
     * // No elements remain because list4 has at least as many occurrences of each value as list3
     * </pre>
     *
     * @param b the list to compare against this list
     * @return a new IntList containing the elements that are present in this list but not in the specified list,
     *         considering the number of occurrences.
     * @see #difference(int[])
     * @see #symmetricDifference(IntList)
     * @see #intersection(IntList)
     * @see N#difference(Collection, Collection)
     * @see N#difference(int[], int[])
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
     * Returns a new list with the elements in this list but not in the specified array {@code b},
     * considering the number of occurrences of each element.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * int[] array = new int[]{2, 5, 1};
     * IntList result = list1.difference(array); // result will be [0, 2, 3]
     * // One '2' remains because list1 has two occurrences and array has one
     *
     * IntList list2 = IntList.of(5, 6);
     * int[] array2 = new int[]{5, 5, 6};
     * IntList result2 = list2.difference(array2); // result will be [] (empty)
     * // No elements remain because array2 has at least as many occurrences of each value as list2
     * </pre>
     *
     * @param b the array to compare against this list
     * @return a new IntList containing the elements that are present in this list but not in the specified array,
     *         considering the number of occurrences.
     *         Returns a copy of this list if {@code b} is {@code null} or empty.
     * @see #difference(IntList)
     * @see #symmetricDifference(int[])
     * @see #intersection(int[])
     * @see N#difference(Collection, Collection)
     * @see N#difference(int[], int[])
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
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both lists.
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
     * @return a new IntList containing elements that are present in either this list or the specified list,
     *         but not in both, considering the number of occurrences
     * @see #symmetricDifference(int[])
     * @see #difference(IntList)
     * @see #intersection(IntList)
     * @see N#symmetricDifference(int[], int[])
     * @see N#symmetricDifference(Collection, Collection)
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
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified array.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * int[] array = new int[]{2, 5, 1};
     * IntList result = list1.symmetricDifference(array);
     * // result will contain: [0, 2, 3, 5]
     * // Elements explanation:
     * // - 0, 3: only in list1
     * // - 5: only in array
     * // - 2: appears twice in list1 and once in array, so one occurrence remains
     * </pre>
     *
     * @param b the array to compare with this list for symmetric difference
     * @return a new IntList containing elements that are present in either this list or the specified array,
     *         but not in both, considering the number of occurrences
     * @see #symmetricDifference(IntList)
     * @see #difference(int[])
     * @see #intersection(int[])
     * @see N#symmetricDifference(int[], int[])
     * @see N#symmetricDifference(Collection, Collection)
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
     * Returns the number of occurrences of the specified value in this list.
     *
     * @param valueToFind the value to count occurrences of
     * @return the number of times the specified value appears in this list
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
     * or -1 if this list does not contain the element.
     *
     * @param valueToFind the element to search for
     * @return the index of the first occurrence of the specified element, or -1 if not found
     */
    public int indexOf(final int valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * starting the search at the specified index, or -1 if the element is not found.
     *
     * @param valueToFind the element to search for
     * @param fromIndex the index to start searching from
     * @return the index of the first occurrence of the specified element starting from fromIndex,
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
     * or -1 if this list does not contain the element.
     *
     * @param valueToFind the element to search for
     * @return the index of the last occurrence of the specified element, or -1 if not found
     */
    public int lastIndexOf(final int valueToFind) {
        return lastIndexOf(valueToFind, size);
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * searching backwards from the specified index, or -1 if the element is not found.
     *
     * @param valueToFind the element to search for
     * @param startIndexFromBack the index to start searching backwards from (inclusive)
     * @return the index of the last occurrence of the specified element, or -1 if not found
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
     * Returns the minimum element in this list.
     *
     * @return an OptionalInt containing the minimum element, or an empty OptionalInt if this list is empty
     */
    public OptionalInt min() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(N.min(elementData, 0, size));
    }

    /**
     * Returns the minimum element in the specified range of this list.
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return an OptionalInt containing the minimum element in the range, or an empty OptionalInt if the range is empty
     * @throws IndexOutOfBoundsException if fromIndex < 0 or toIndex > size() or fromIndex > toIndex
     */
    public OptionalInt min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalInt.empty() : OptionalInt.of(N.min(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the maximum element in this list.
     *
     * @return an OptionalInt containing the maximum element, or an empty OptionalInt if this list is empty
     */
    public OptionalInt max() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(N.max(elementData, 0, size));
    }

    /**
     * Returns the maximum element in the specified range of this list.
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return an OptionalInt containing the maximum element in the range, or an empty OptionalInt if the range is empty
     * @throws IndexOutOfBoundsException if fromIndex < 0 or toIndex > size() or fromIndex > toIndex
     */
    public OptionalInt max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalInt.empty() : OptionalInt.of(N.max(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the median element of this list.
     * If the list has an even number of elements, returns the lower median.
     *
     * @return an OptionalInt containing the median element, or an empty OptionalInt if this list is empty
     */
    public OptionalInt median() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(N.median(elementData, 0, size));
    }

    /**
     * Returns the median element in the specified range of this list.
     * If the range has an even number of elements, returns the lower median.
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return an OptionalInt containing the median element in the range, or an empty OptionalInt if the range is empty
     * @throws IndexOutOfBoundsException if fromIndex < 0 or toIndex > size() or fromIndex > toIndex
     */
    public OptionalInt median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalInt.empty() : OptionalInt.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     * Performs the given action for each element of this list.
     *
     * @param action the action to be performed for each element
     */
    public void forEach(final IntConsumer action) {
        forEach(0, size, action);
    }

    /**
     * Performs the given action for each element in the specified range of this list.
     * If fromIndex is greater than toIndex, the elements are processed in reverse order.
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive), or -1 to process in reverse from fromIndex to 0
     * @param action the action to be performed for each element
     * @throws IndexOutOfBoundsException if the range is out of bounds
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

    //    /**
    //     *
    //     * @param <E>
    //     * @param action
    //     * @throws E the e
    //     */
    //    public <E extends Exception> void forEachIndexed(final Throwables.IntIntConsumer<E> action) throws E {
    //        forEachIndexed(0, size, action);
    //    }
    //
    //    /**
    //     *
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param action
    //     * @throws IndexOutOfBoundsException
    //     */
    //    public <E extends Exception> void forEachIndexed(final int fromIndex, final int toIndex, final Throwables.IntIntConsumer<E> action)
    //            throws IndexOutOfBoundsException, E {
    //        N.checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex), size);
    //
    //        if (size > 0) {
    //            if (fromIndex <= toIndex) {
    //                for (int i = fromIndex; i < toIndex; i++) {
    //                    action.accept(i, elementData[i]);
    //                }
    //            } else {
    //                for (int i = N.min(size - 1, fromIndex); i > toIndex; i--) {
    //                    action.accept(i, elementData[i]);
    //                }
    //            }
    //        }
    //    }

    /**
     * Returns the first element in this list.
     *
     * @return an OptionalInt containing the first element, or an empty OptionalInt if this list is empty
     */
    public OptionalInt first() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(elementData[0]);
    }

    /**
     * Returns the last element in this list.
     *
     * @return an OptionalInt containing the last element, or an empty OptionalInt if this list is empty
     */
    public OptionalInt last() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(elementData[size() - 1]);
    }

    /**
     * Returns a new IntList containing only the distinct elements from the specified range.
     * The order of elements is preserved (keeping the first occurrence of each distinct element).
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a new IntList containing the distinct elements
     * @throws IndexOutOfBoundsException if fromIndex < 0 or toIndex > size() or fromIndex > toIndex
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
     * Checks if this list contains any duplicate elements.
     *
     * @return true if there are duplicate elements in this list
     */
    @Override
    public boolean hasDuplicates() {
        return N.hasDuplicates(elementData, 0, size, false);
    }

    /**
     * Checks if this list is sorted in ascending order.
     *
     * @return true if this list is sorted in ascending order
     */
    @Override
    public boolean isSorted() {
        return N.isSorted(elementData, 0, size);
    }

    /**
     * Sorts this list in ascending order.
     */
    @Override
    public void sort() {
        if (size > 1) {
            N.sort(elementData, 0, size);
        }
    }

    /**
     * Sorts this list in ascending order using parallel sorting algorithm.
     * This method may be faster than sort() for large lists on multi-core systems.
     */
    public void parallelSort() {
        if (size > 1) {
            N.parallelSort(elementData, 0, size);
        }
    }

    /**
     * Sorts this list in descending order.
     */
    @Override
    public void reverseSort() {
        if (size > 1) {
            sort();
            reverse();
        }
    }

    /**
     * Searches for the specified value using the binary search algorithm.
     * The list must be sorted in ascending order prior to making this call.
     *
     * @param valueToFind the value to search for
     * @return the index of the search key if found, otherwise (-(insertion point) - 1)
     */
    public int binarySearch(final int valueToFind) {
        return N.binarySearch(elementData, valueToFind);
    }

    /**
     * Searches for the specified value in the specified range using the binary search algorithm.
     * The range must be sorted in ascending order prior to making this call.
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @param valueToFind the value to search for
     * @return the index of the search key if found, otherwise (-(insertion point) - 1)
     * @throws IndexOutOfBoundsException if fromIndex < 0 or toIndex > size() or fromIndex > toIndex
     */
    public int binarySearch(final int fromIndex, final int toIndex, final int valueToFind) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return N.binarySearch(elementData, fromIndex, toIndex, valueToFind);
    }

    /**
     * Reverse.
     */
    @Override
    public void reverse() {
        if (size > 1) {
            N.reverse(elementData, 0, size);
        }
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    @Override
    public void reverse(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            N.reverse(elementData, fromIndex, toIndex);
        }
    }

    /**
     *
     * @param distance
     */
    @Override
    public void rotate(final int distance) {
        if (size > 1) {
            N.rotate(elementData, 0, size, distance);
        }
    }

    /**
     * Shuffle.
     */
    @Override
    public void shuffle() {
        if (size() > 1) {
            N.shuffle(elementData, 0, size);
        }
    }

    /**
     *
     * @param rnd
     */
    @Override
    public void shuffle(final Random rnd) {
        if (size() > 1) {
            N.shuffle(elementData, 0, size, rnd);
        }
    }

    /**
     *
     * @param i
     * @param j
     */
    @Override
    public void swap(final int i, final int j) {
        rangeCheck(i);
        rangeCheck(j);

        set(i, set(j, elementData[i]));
    }

    @Override
    public IntList copy() {
        return new IntList(N.copyOfRange(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    @Override
    public IntList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new IntList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     * @throws IndexOutOfBoundsException
     * @see N#copyOfRange(int[], int, int, int)
     */
    @Override
    public IntList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex));

        return new IntList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Returns List of {@code IntList} with consecutive subsequences of the elements, each of the same size (the final sequence may be smaller).
     *
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each subsequence (the last may be smaller).
     * @return
     * @throws IndexOutOfBoundsException
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

    //    @Override
    //    public List<IntList> split(int fromIndex, int toIndex, IntPredicate predicate) {
    //        checkIndex(fromIndex, toIndex);
    //
    //        final List<IntList> result = new ArrayList<>();
    //        IntList piece = null;
    //
    //        for (int i = fromIndex; i < toIndex;) {
    //            if (piece == null) {
    //                piece = IntList.of(N.EMPTY_INT_ARRAY);
    //            }
    //
    //            if (predicate.test(elementData[i])) {
    //                piece.add(elementData[i]);
    //                i++;
    //            } else {
    //                result.add(piece);
    //                piece = null;
    //            }
    //        }
    //
    //        if (piece != null) {
    //            result.add(piece);
    //        }
    //
    //        return result;
    //    }

    /**
     * Trim to size.
     *
     * @return
     */
    @Override
    public IntList trimToSize() {
        if (elementData.length > size) {
            elementData = N.copyOfRange(elementData, 0, size);
        }

        return this;
    }

    /**
     * Clear.
     */
    @Override
    public void clear() {
        if (size > 0) {
            N.fill(elementData, 0, size, 0);
        }

        size = 0;
    }

    /**
     * Checks if is empty.
     *
     * @return {@code true}, if is empty
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public List<Integer> boxed() {
        return boxed(0, size);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
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

    @Override
    public int[] toArray() {
        return N.copyOfRange(elementData, 0, size);
    }

    /**
     * To long list.
     *
     * @return
     */
    public LongList toLongList() {
        final long[] a = new long[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i];//NOSONAR
        }

        return LongList.of(a);
    }

    /**
     * To float list.
     *
     * @return
     */
    public FloatList toFloatList() {
        final float[] a = new float[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i];//NOSONAR
        }

        return FloatList.of(a);
    }

    /**
     * To double list.
     *
     * @return
     */
    public DoubleList toDoubleList() {
        final double[] a = new double[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i];//NOSONAR
        }

        return DoubleList.of(a);
    }

    /**
     *
     * @param <C>
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
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
     *
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
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

    @Override
    public IntIterator iterator() {
        if (isEmpty()) {
            return IntIterator.EMPTY;
        }

        return IntIterator.of(elementData, 0, size);
    }

    public IntStream stream() {
        return IntStream.of(elementData, 0, size());
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public IntStream stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return IntStream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in the list.
     *
     * @return The first int value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public int getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in the list.
     *
     * @return The last int value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public int getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list.
     *
     * @param e the element to add
     */
    public void addFirst(final int e) {
        add(0, e);
    }

    /**
     * Inserts the specified element at the end of this list.
     *
     * @param e the element to add
     */
    public void addLast(final int e) {
        add(size, e);
    }

    /**
     * Removes and returns the first element from this list.
     *
     * @return The first int value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public int removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(0);
    }

    /**
     * Removes and returns the last element from this list.
     *
     * @return The last int value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public int removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(size - 1);
    }

    //    /**
    //     * Returns a new IntList with the elements in reverse order.
    //     *
    //     * @return A new IntList with all elements of the current list in reverse order.
    //     */
    //    public IntList reversed() {
    //        final int[] a = N.copyOfRange(elementData, 0, size);
    //
    //        N.reverse(a);
    //
    //        return new IntList(a);
    //    }
    //
    //    /**
    //     *
    //     * @param <R>
    //     * @param <E>
    //     * @param func
    //     * @return
    //     * @throws E the e
    //     */
    //    @Override
    //    public <R, E extends Exception> R apply(final Throwables.Function<? super IntList, ? extends R, E> func) throws E {
    //        return func.apply(this);
    //    }
    //
    //    /**
    //     * Apply if not empty.
    //     *
    //     * @param <R>
    //     * @param <E>
    //     * @param func
    //     * @return
    //     * @throws E the e
    //     */
    //    @Override
    //    public <R, E extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super IntList, ? extends R, E> func) throws E {
    //        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    //    }
    //
    //    /**
    //     *
    //     * @param <E>
    //     * @param action
    //     * @throws E the e
    //     */
    //    @Override
    //    public <E extends Exception> void accept(final Throwables.Consumer<? super IntList, E> action) throws E {
    //        action.accept(this);
    //    }
    //
    //    /**
    //     * Accept if not empty.
    //     *
    //     * @param <E>
    //     * @param action
    //     * @return
    //     * @throws E the e
    //     */
    //    @Override
    //    public <E extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super IntList, E> action) throws E {
    //        return If.is(size > 0).then(this, action);
    //    }

    @Override
    public int hashCode() {
        return N.hashCode(elementData, 0, size);
    }

    /**
     *
     * @param obj
     * @return
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

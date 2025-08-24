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
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharUnaryOperator;
import com.landawn.abacus.util.stream.CharStream;

/**
 * A resizable, primitive char array implementation of a list.
 * This class provides a more memory-efficient alternative to {@code List<Character>}
 * when working with large collections of char values.
 * 
 * <p>Like {@code ArrayList}, the size, isEmpty, get, set, iterator, and listIterator
 * operations run in constant time. The add operation runs in amortized constant time,
 * that is, adding n elements requires O(n) time. All other operations run in linear time.
 * 
 * <p>Each CharList instance has a capacity. The capacity is the size of the array used
 * to store the elements in the list. It is always at least as large as the list size.
 * As elements are added to a CharList, its capacity grows automatically.
 * 
 * <p>An application can increase the capacity of a CharList instance before adding a
 * large number of elements using the ensureCapacity operation. This may reduce the
 * amount of incremental reallocation.
 * 
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a CharList instance concurrently, and at least one of
 * the threads modifies the list structurally, it must be synchronized externally.
 * 
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 */
public final class CharList extends PrimitiveList<Character, char[], CharList> {

    @Serial
    private static final long serialVersionUID = 7293826835233022514L;

    static final Random RAND = new SecureRandom();

    private char[] elementData = N.EMPTY_CHAR_ARRAY;

    private int size = 0;

    /**
     * Constructs an empty CharList with the default initial capacity (10).
     */
    public CharList() {
    }

    /**
     * Constructs an empty CharList with the specified initial capacity.
     * 
     * @param initialCapacity the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    public CharList(final int initialCapacity) {
        N.checkArgNotNegative(initialCapacity, cs.initialCapacity);

        elementData = initialCapacity == 0 ? N.EMPTY_CHAR_ARRAY : new char[initialCapacity];
    }

    /**
     * Constructs a CharList containing the elements of the specified array.
     * The CharList instance uses the specified array as its backing array without copying.
     * Changes to the array will be reflected in the list and vice versa.
     * 
     * @param a the array whose elements are to be used as the backing array for this list
     */
    public CharList(final char[] a) {
        this(N.requireNonNull(a), a.length);
    }

    /**
     * Constructs a CharList using the specified array as the element array for this list without copying action.
     * The first {@code size} elements of the array will be used as the initial elements of the list.
     * Changes to the array will be reflected in the list and vice versa.
     * 
     * @param a the array to be used as the element array for this list
     * @param size the number of elements in the list
     * @throws IndexOutOfBoundsException if the specified size is negative or greater than the array length
     */
    public CharList(final char[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates a new CharList containing the specified char values.
     * 
     * @param a the char values to be placed into this list
     * @return a new CharList containing the specified values
     */
    public static CharList of(final char... a) {
        return new CharList(N.nullToEmpty(a));
    }

    /**
     * Creates a new CharList containing the first {@code size} elements of the specified array.
     * 
     * @param a the array whose elements are to be placed into this list
     * @param size the number of elements from the array to include in the list
     * @return a new CharList containing the specified elements
     * @throws IndexOutOfBoundsException if size is negative or greater than the array length
     */
    public static CharList of(final char[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new CharList(N.nullToEmpty(a), size);
    }

    /**
     * Creates a new CharList that is a copy of the specified array.
     * Modifications to the returned list will not affect the original array.
     * 
     * @param a the array to copy
     * @return a new CharList containing a copy of the specified array's elements
     */
    public static CharList copyOf(final char[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates a new CharList that is a copy of the specified range of the array.
     * The returned list will contain elements from index {@code fromIndex} (inclusive)
     * to index {@code toIndex} (exclusive).
     * 
     * @param a the array from which a range is to be copied
     * @param fromIndex the initial index of the range to be copied, inclusive
     * @param toIndex the final index of the range to be copied, exclusive
     * @return a new CharList containing the specified range of the array
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > a.length} or {@code fromIndex > toIndex}
     */
    public static CharList copyOf(final char[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates a CharList containing a sequence of char values from startInclusive (inclusive) to endExclusive (exclusive).
     * 
     * <p>For example, {@code range('a', 'd')} returns a list containing ['a', 'b', 'c'].
     * 
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a new CharList containing the range of values
     */
    public static CharList range(final char startInclusive, final char endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     * Creates a CharList containing a sequence of char values from startInclusive (inclusive) to endExclusive (exclusive),
     * incrementing by the specified step.
     * 
     * <p>For example, {@code range('a', 'g', 2)} returns a list containing ['a', 'c', 'e'].
     * 
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param by the step size (must be positive)
     * @return a new CharList containing the range of values
     * @throws IllegalArgumentException if {@code by} is zero or negative
     */
    public static CharList range(final char startInclusive, final char endExclusive, final int by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     * Creates a CharList containing a sequence of char values from startInclusive to endInclusive (both inclusive).
     * 
     * <p>For example, {@code rangeClosed('a', 'd')} returns a list containing ['a', 'b', 'c', 'd'].
     * 
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a new CharList containing the range of values
     */
    public static CharList rangeClosed(final char startInclusive, final char endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     * Creates a CharList containing a sequence of char values from startInclusive to endInclusive (both inclusive),
     * incrementing by the specified step.
     * 
     * <p>For example, {@code rangeClosed('a', 'g', 2)} returns a list containing ['a', 'c', 'e', 'g'].
     * 
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @param by the step size (must be positive)
     * @return a new CharList containing the range of values
     * @throws IllegalArgumentException if {@code by} is zero or negative
     */
    public static CharList rangeClosed(final char startInclusive, final char endInclusive, final int by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     * Creates a CharList containing the specified element repeated {@code len} times.
     * 
     * <p>For example, {@code repeat('a', 3)} returns a list containing ['a', 'a', 'a'].
     * 
     * @param element the element to repeat
     * @param len the number of times to repeat the element
     * @return a new CharList containing the repeated element
     * @throws IllegalArgumentException if {@code len} is negative
     */
    public static CharList repeat(final char element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates a CharList of the specified length filled with random char values.
     * Each character will be randomly generated from the entire range of possible char values (0 to 65535).
     * 
     * @param len the length of the list to create
     * @return a new CharList containing random char values
     * @throws IllegalArgumentException if {@code len} is negative
     */
    public static CharList random(final int len) {
        final char[] a = new char[len];
        final int mod = Character.MAX_VALUE + 1;

        for (int i = 0; i < len; i++) {
            a[i] = (char) RAND.nextInt(mod);
        }

        return of(a);
    }

    /**
     * Creates a CharList of the specified length filled with random char values within the specified range.
     * Each character will be randomly generated from startInclusive (inclusive) to endExclusive (exclusive).
     * 
     * @param startInclusive the minimum value (inclusive)
     * @param endExclusive the maximum value (exclusive)
     * @param len the length of the list to create
     * @return a new CharList containing random char values within the specified range
     * @throws IllegalArgumentException if {@code startInclusive >= endExclusive} or {@code len} is negative
     */
    public static CharList random(final char startInclusive, final char endExclusive, final int len) {
        if (startInclusive >= endExclusive) {
            throw new IllegalArgumentException("'startInclusive' must be less than 'endExclusive'");
        }

        final char[] a = new char[len];
        final int mod = endExclusive - startInclusive;

        for (int i = 0; i < len; i++) {
            a[i] = (char) (RAND.nextInt(mod) + startInclusive);
        }

        return of(a);
    }

    /**
     * Creates a CharList of the specified length by randomly selecting from the provided candidate chars.
     * Each element in the returned list is randomly chosen from the candidates array.
     * 
     * @param candidates the array of candidate chars to choose from
     * @param len the length of the list to create
     * @return a new CharList containing randomly selected chars from the candidates
     * @throws IllegalArgumentException if candidates is empty or has Integer.MAX_VALUE elements, or if {@code len} is negative
     */
    public static CharList random(final char[] candidates, final int len) {
        if (N.isEmpty(candidates) || candidates.length == Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        } else if (candidates.length == 1) {
            return repeat(candidates[0], len);
        }

        final int n = candidates.length;
        final char[] a = new char[len];

        for (int i = 0; i < len; i++) {
            a[i] = candidates[RAND.nextInt(n)];
        }

        return of(a);
    }

    /**
     * Returns the underlying char array backing this list.
     * Changes to the returned array will be reflected in the list and vice versa.
     * Use with caution as it breaks encapsulation.
     * 
     * @return the underlying char array
     * @deprecated should call {@code toArray()}
     */
    @Beta
    @Deprecated
    @Override
    public char[] array() {
        return elementData;
    }

    /**
     * Returns the char value at the specified position in this list.
     * 
     * @param index the index of the element to return
     * @return the char value at the specified position
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size())
     */
    public char get(final int index) {
        rangeCheck(index);

        return elementData[index];
    }

    /**
     * Checks if the specified index is valid for accessing an element.
     * 
     * @param index the index to check
     * @throws IndexOutOfBoundsException if the index is out of range
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
    public char set(final int index, final char e) {
        rangeCheck(index);

        final char oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     * Appends the specified element to the end of this list.
     * 
     * @param e the element to be appended to this list
     */
    public void add(final char e) {
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
    public void add(final int index, final char e) {
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
     * Appends all of the elements in the specified CharList to the end of this list,
     * in the order that they are returned by the specified collection's iterator.
     * 
     * @param c the CharList containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean addAll(final CharList c) {
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
     * Inserts all of the elements in the specified CharList into this list,
     * starting at the specified position. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (increases their indices).
     * 
     * @param index the index at which to insert the first element from the specified collection
     * @param c the CharList containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > size())
     */
    @Override
    public boolean addAll(final int index, final CharList c) {
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
     * Appends all of the elements in the specified array to the end of this list.
     * 
     * @param a the array containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean addAll(final char[] a) {
        return addAll(size(), a);
    }

    /**
     * Inserts all of the elements in the specified array into this list,
     * starting at the specified position. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right.
     * 
     * @param index the index at which to insert the first element from the specified array
     * @param a the array containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > size())
     */
    @Override
    public boolean addAll(final int index, final char[] a) {
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
     * @param e the element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     */
    public boolean remove(final char e) {
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
     * The list is compacted after removal, maintaining the order of remaining elements.
     * 
     * @param e the element to be removed from this list
     * @return {@code true} if this list was modified (at least one element was removed)
     */
    public boolean removeAllOccurrences(final char e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] != e) {
                elementData[w++] = elementData[i];
            }
        }

        final int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, (char) 0);

            size = w;
        }

        return numRemoved > 0;
    }

    /**
     * Private method to remove the element at the specified position without range checking.
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
     * Removes from this list all of its elements that are contained in the specified CharList.
     * 
     * @param c the CharList containing elements to be removed from this list
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean removeAll(final CharList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        return batchRemove(c, false) > 0;
    }

    /**
     * Removes from this list all of its elements that are contained in the specified array.
     * 
     * @param a the array containing elements to be removed from this list
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean removeAll(final char[] a) {
        if (N.isEmpty(a)) {
            return false;
        }

        return removeAll(of(a));
    }

    /**
     * Removes all of the elements of this list that satisfy the given predicate.
     * Errors or runtime exceptions thrown during iteration or by the predicate
     * are relayed to the caller.
     * 
     * @param p a predicate which returns {@code true} for elements to be removed
     * @return {@code true} if any elements were removed
     */
    public boolean removeIf(final CharPredicate p) {
        final CharList tmp = new CharList(size());

        for (int i = 0; i < size; i++) {
            if (!p.test(elementData[i])) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, elementData, 0, tmp.size());
        N.fill(elementData, tmp.size(), size, (char) 0);
        size = tmp.size;

        return true;
    }

    /**
     * Removes all duplicate elements from this list, keeping only the first occurrence of each element.
     * The order of elements is preserved.
     * 
     * @return {@code true} if any duplicates were removed
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
            final Set<Character> set = N.newLinkedHashSet(size);
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
            N.fill(elementData, idx + 1, size, (char) 0);

            size = idx + 1;
            return true;
        }
    }

    /**
     * Retains only the elements in this list that are contained in the specified CharList.
     * In other words, removes from this list all of its elements that are not contained
     * in the specified CharList.
     * 
     * @param c the CharList containing elements to be retained in this list
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean retainAll(final CharList c) {
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
     * @param a the array containing elements to be retained in this list
     * @return {@code true} if this list changed as a result of the call
     */
    @Override
    public boolean retainAll(final char[] a) {
        if (N.isEmpty(a)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(CharList.of(a));
    }

    /**
     * Removes or retains elements based on whether they are contained in the specified CharList.
     * 
     * @param c the CharList to check against
     * @param complement if {@code false}, removes elements in c; if {@code true}, retains only elements in c
     * @return the number of elements removed
     */
    private int batchRemove(final CharList c, final boolean complement) {
        final char[] elementData = this.elementData;//NOSONAR

        int w = 0;

        if (c.size() > 3 && size() > 9) {
            final Set<Character> set = c.toSet();

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
            N.fill(elementData, w, size, (char) 0);

            size = w;
        }

        return numRemoved;
    }

    /**
     * Removes the element at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their indices).
     * 
     * @param index the index of the element to be removed
     * @return the element that was removed from the list
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size())
     */
    public char delete(final int index) {
        rangeCheck(index);

        final char oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     * Removes all elements at the specified indices from this list.
     * The indices array will be sorted internally, and duplicates will be removed.
     * Elements are removed in descending order of indices to maintain correctness.
     * 
     * @param indices the indices of elements to be removed
     */
    @Override
    public void deleteAllByIndices(final int... indices) {
        if (N.isEmpty(indices)) {
            return;
        }

        final char[] tmp = N.deleteAllByIndices(elementData, indices);

        N.copy(tmp, 0, elementData, 0, tmp.length);

        if (size > tmp.length) {
            N.fill(elementData, tmp.length, size, (char) 0);
        }

        size -= elementData.length - tmp.length;
    }

    /**
     * Removes from this list all of the elements whose index is between
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * Shifts any succeeding elements to the left (reduces their index).
     * 
     * @param fromIndex the index of the first element to be removed
     * @param toIndex the index after the last element to be removed
     * @throws IndexOutOfBoundsException if {@code fromIndex} or {@code toIndex} is out of range
     *         ({@code fromIndex < 0 || toIndex > size() || fromIndex > toIndex})
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

        N.fill(elementData, newSize, size, (char) 0);

        this.size = newSize;
    }

    /**
     * Moves a range of elements within this list to a new position.
     * The elements from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
     * are moved to start at position {@code newPositionStartIndexAfterMove}.
     * 
     * <p>No elements are deleted in the process; this list maintains its size.
     * Elements are shifted as necessary to accommodate the move.
     * 
     * @param fromIndex the starting index (inclusive) of the range to be moved
     * @param toIndex the ending index (exclusive) of the range to be moved
     * @param newPositionStartIndexAfterMove the start index where the range should be positioned after the move
     * @throws IndexOutOfBoundsException if the range is out of bounds or if
     *         {@code newPositionStartIndexAfterMove} would result in elements being moved outside the list bounds
     */
    @Override
    public void moveRange(final int fromIndex, final int toIndex, final int newPositionStartIndexAfterMove) {
        N.moveRange(elementData, fromIndex, toIndex, newPositionStartIndexAfterMove);
    }

    /**
     * Replaces each element in the specified range of this list with elements
     * from the replacement CharList. The range extends from {@code fromIndex} (inclusive)
     * to {@code toIndex} (exclusive).
     * 
     * @param fromIndex the index of the first element to be replaced
     * @param toIndex the index after the last element to be replaced
     * @param replacement the CharList whose elements will replace the specified range
     * @throws IndexOutOfBoundsException if the range is out of bounds
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final CharList replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, (char) 0);
        }

        this.size = newSize;
    }

    /**
     * Replaces each element in the specified range of this list with elements
     * from the replacement array. The range extends from {@code fromIndex} (inclusive)
     * to {@code toIndex} (exclusive).
     * 
     * @param fromIndex the index of the first element to be replaced
     * @param toIndex the index after the last element to be replaced
     * @param replacement the array whose elements will replace the specified range
     * @throws IndexOutOfBoundsException if the range is out of bounds
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final char[] replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, (char) 0);
        }

        this.size = newSize;
    }

    /**
     * Replaces all occurrences of the specified value with the new value in this list.
     * 
     * @param oldVal the old value to be replaced
     * @param newVal the new value to replace the old value
     * @return the number of elements replaced
     */
    public int replaceAll(final char oldVal, final char newVal) {
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
    public void replaceAll(final CharUnaryOperator operator) {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsChar(elementData[i]);
        }
    }

    /**
     * Replaces all elements that satisfy the given predicate with the specified new value.
     * 
     * @param predicate the predicate to test elements
     * @param newValue the value to replace matching elements with
     * @return {@code true} if any elements were replaced
     */
    public boolean replaceIf(final CharPredicate predicate, final char newValue) {
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
    public void fill(final char val) {
        fill(0, size(), val);
    }

    /**
     * Replaces the elements in the specified range of this list with the specified value.
     * The range extends from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     * 
     * @param fromIndex the index of the first element (inclusive) to be filled with the specified value
     * @param toIndex the index after the last element (exclusive) to be filled with the specified value
     * @param val the value to be stored in the specified range
     * @throws IndexOutOfBoundsException if the range is out of bounds
     */
    public void fill(final int fromIndex, final int toIndex, final char val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     * 
     * @param valueToFind the element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element
     */
    public boolean contains(final char valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     * Returns {@code true} if this list contains any element from the specified CharList.
     * 
     * @param c the CharList to check for common elements
     * @return {@code true} if this list contains at least one element from the specified CharList
     */
    @Override
    public boolean containsAny(final CharList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    /**
     * Returns {@code true} if this list contains any element from the specified array.
     * 
     * @param a the array to check for common elements
     * @return {@code true} if this list contains at least one element from the specified array
     */
    @Override
    public boolean containsAny(final char[] a) {
        if (isEmpty() || N.isEmpty(a)) {
            return false;
        }

        return !disjoint(a);
    }

    /**
     * Returns {@code true} if this list contains all of the elements in the specified CharList.
     * 
     * @param c the CharList to be checked for containment in this list
     * @return {@code true} if this list contains all of the elements in the specified CharList
     */
    @Override
    public boolean containsAll(final CharList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        if (needToSet(size(), c.size())) {
            final Set<Character> set = this.toSet();

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
     * @param a the array to be checked for containment in this list
     * @return {@code true} if this list contains all of the elements in the specified array
     */
    @Override
    public boolean containsAll(final char[] a) {
        if (N.isEmpty(a)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        return containsAll(of(a));
    }

    /**
     * Returns {@code true} if this list has no elements in common with the specified CharList.
     * Two lists are disjoint if they have no elements in common.
     * 
     * @param c the CharList to check for common elements
     * @return {@code true} if the two lists have no elements in common
     */
    @Override
    public boolean disjoint(final CharList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        if (needToSet(size(), c.size())) {
            final Set<Character> set = this.toSet();

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
     * Returns {@code true} if this list has no elements in common with the specified array.
     * 
     * @param b the array to check for common elements
     * @return {@code true} if this list and the array have no elements in common
     */
    @Override
    public boolean disjoint(final char[] b) {
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
     * CharList list1 = CharList.of('a', 'a', 'b', 'c');
     * CharList list2 = CharList.of('a', 'b', 'b', 'd');
     * CharList result = list1.intersection(list2); // result will be ['a', 'b']
     * // One occurrence of 'a' (minimum count in both lists) and one occurrence of 'b'
     *
     * CharList list3 = CharList.of('x', 'x', 'y');
     * CharList list4 = CharList.of('x', 'z');
     * CharList result2 = list3.intersection(list4); // result will be ['x']
     * // One occurrence of 'x' (minimum count in both lists)
     * </pre>
     *
     * @param b the list to find common elements with this list
     * @return a new CharList containing elements present in both this list and the specified list,
     *         considering the minimum number of occurrences in either list.
     *         Returns an empty list if either list is {@code null} or empty.
     * @see #intersection(char[])
     * @see #difference(CharList)
     * @see #symmetricDifference(CharList)
     * @see N#intersection(char[], char[])
     * @see N#intersection(int[], int[])
     */
    @Override
    public CharList intersection(final CharList b) {
        if (N.isEmpty(b)) {
            return new CharList();
        }

        final Multiset<Character> bOccurrences = b.toMultiset();

        final CharList c = new CharList(N.min(9, size(), b.size()));

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
     * CharList list1 = CharList.of('a', 'a', 'b', 'c');
     * char[] array = new char[]{'a', 'b', 'b', 'd'};
     * CharList result = list1.intersection(array); // result will be ['a', 'b']
     * // One occurrence of 'a' (minimum count in both sources) and one occurrence of 'b'
     *
     * CharList list2 = CharList.of('x', 'x', 'y');
     * char[] array2 = new char[]{'x', 'z'};
     * CharList result2 = list2.intersection(array2); // result will be ['x']
     * // One occurrence of 'x' (minimum count in both sources)
     * </pre>
     *
     * @param b the array to find common elements with this list
     * @return a new CharList containing elements present in both this list and the specified array,
     *         considering the minimum number of occurrences in either source.
     *         Returns an empty list if the array is {@code null} or empty.
     * @see #intersection(CharList)
     * @see #difference(char[])
     * @see #symmetricDifference(char[])
     * @see N#intersection(char[], char[])
     * @see N#intersection(int[], int[])
     */
    @Override
    public CharList intersection(final char[] b) {
        if (N.isEmpty(b)) {
            return new CharList();
        }

        return intersection(of(b));
    }

    /**
     * Returns a new list with the elements in this list but not in the specified list {@code b},
     * considering the number of occurrences of each element.
     *
     * <p>Example:
     * <pre>
     * CharList list1 = CharList.of('a', 'a', 'b', 'c');
     * CharList list2 = CharList.of('a', 'd');
     * CharList result = list1.difference(list2); // result will be ['a', 'b', 'c']
     * // One 'a' remains because list1 has two occurrences and list2 has one
     *
     * CharList list3 = CharList.of('e', 'f');
     * CharList list4 = CharList.of('e', 'e', 'f');
     * CharList result2 = list3.difference(list4); // result will be [] (empty)
     * // No elements remain because list4 has at least as many occurrences of each value as list3
     * </pre>
     *
     * @param b the list to compare against this list
     * @return a new CharList containing the elements that are present in this list but not in the specified list,
     *         considering the number of occurrences.
     * @see #difference(char[])
     * @see #symmetricDifference(CharList)
     * @see #intersection(CharList)
     * @see N#difference(char[], char[])
     * @see N#difference(int[], int[])
     */
    @Override
    public CharList difference(final CharList b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Character> bOccurrences = b.toMultiset();

        final CharList c = new CharList(N.min(size(), N.max(9, size() - b.size())));

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
     * CharList list1 = CharList.of('a', 'a', 'b', 'c');
     * char[] array = new char[]{'a', 'd'};
     * CharList result = list1.difference(array); // result will be ['a', 'b', 'c']
     * // One 'a' remains because list1 has two occurrences and array has one
     *
     * CharList list2 = CharList.of('e', 'f');
     * char[] array2 = new char[]{'e', 'e', 'f'};
     * CharList result2 = list2.difference(array2); // result will be [] (empty)
     * // No elements remain because array2 has at least as many occurrences of each value as list2
     * </pre>
     *
     * @param b the array to compare against this list
     * @return a new CharList containing the elements that are present in this list but not in the specified array,
     *         considering the number of occurrences.
     *         Returns a copy of this list if {@code b} is {@code null} or empty.
     * @see #difference(CharList)
     * @see #symmetricDifference(char[])
     * @see #intersection(char[])
     * @see N#difference(char[], char[])
     * @see N#difference(int[], int[])
     */
    @Override
    public CharList difference(final char[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(b));
    }

    /**
     * Returns a new list containing the symmetric difference between this list and the specified CharList.
     * The symmetric difference consists of elements that are in either this list or the specified list,
     * but not in both. Occurrences are considered.
     * 
     * <p>Example:
     * <pre>
     * CharList list1 = CharList.of('a', 'b', 'c');
     * CharList list2 = CharList.of('b', 'c', 'd');
     * CharList result = list1.symmetricDifference(list2); // result will be ['a', 'd']
     * </pre>
     * 
     * @param b the CharList to find the symmetric difference with
     * @return a new CharList containing elements that are in either list but not in both
     * @see IntList#symmetricDifference(IntList)
     */
    @Override
    public CharList symmetricDifference(final CharList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (isEmpty()) {
            return b.copy();
        }

        final Multiset<Character> bOccurrences = b.toMultiset();
        final CharList c = new CharList(N.max(9, Math.abs(size() - b.size())));

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
     * Returns a new list containing the symmetric difference between this list and the specified array.
     * The symmetric difference consists of elements that are in either this list or the specified array,
     * but not in both. Occurrences are considered.
     * 
     * @param b the array to find the symmetric difference with
     * @return a new CharList containing elements that are in either the list or array but not in both
     * @see IntList#symmetricDifference(IntList)
     */
    @Override
    public CharList symmetricDifference(final char[] b) {
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
     * @param valueToFind the value whose occurrences are to be counted
     * @return the number of times the specified value appears in this list
     */
    public int occurrencesOf(final char valueToFind) {
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
     * @return the index of the first occurrence of the specified element in this list,
     *         or -1 if this list does not contain the element
     */
    public int indexOf(final char valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * starting the search at the specified index, or -1 if the element is not found.
     * 
     * @param valueToFind the element to search for
     * @param fromIndex the index to start the search from (inclusive)
     * @return the index of the first occurrence of the element in this list at position >= fromIndex,
     *         or -1 if the element is not found
     */
    public int indexOf(final char valueToFind, final int fromIndex) {
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
     * @return the index of the last occurrence of the specified element in this list,
     *         or -1 if this list does not contain the element
     */
    public int lastIndexOf(final char valueToFind) {
        return lastIndexOf(valueToFind, size);
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * searching backwards from the specified index, or -1 if the element is not found.
     * 
     * @param valueToFind the element to search for
     * @param startIndexFromBack the index to start the backward search from (inclusive)
     * @return the index of the last occurrence of the element at position <= startIndexFromBack,
     *         or -1 if the element is not found
     */
    public int lastIndexOf(final char valueToFind, final int startIndexFromBack) {
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
     * @return an OptionalChar containing the minimum element, or an empty OptionalChar if this list is empty
     */
    public OptionalChar min() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(N.min(elementData, 0, size));
    }

    /**
     * Returns the minimum element in the specified range of this list.
     * 
     * @param fromIndex the index of the first element (inclusive) to be included in the search
     * @param toIndex the index of the last element (exclusive) to be included in the search
     * @return an OptionalChar containing the minimum element in the specified range,
     *         or an empty OptionalChar if the range is empty
     * @throws IndexOutOfBoundsException if the range is out of bounds
     */
    public OptionalChar min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalChar.empty() : OptionalChar.of(N.min(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the maximum element in this list.
     * 
     * @return an OptionalChar containing the maximum element, or an empty OptionalChar if this list is empty
     */
    public OptionalChar max() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(N.max(elementData, 0, size));
    }

    /**
     * Returns the maximum element in the specified range of this list.
     * 
     * @param fromIndex the index of the first element (inclusive) to be included in the search
     * @param toIndex the index of the last element (exclusive) to be included in the search
     * @return an OptionalChar containing the maximum element in the specified range,
     *         or an empty OptionalChar if the range is empty
     * @throws IndexOutOfBoundsException if the range is out of bounds
     */
    public OptionalChar max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalChar.empty() : OptionalChar.of(N.max(elementData, fromIndex, toIndex));
    }

    /**
     * Returns the median element in this list.
     * If the list has an even number of elements, returns the lower median.
     * 
     * @return an OptionalChar containing the median element, or an empty OptionalChar if this list is empty
     */
    public OptionalChar median() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(N.median(elementData, 0, size));
    }

    /**
     * Returns the median element in the specified range of this list.
     * If the range has an even number of elements, returns the lower median.
     * 
     * @param fromIndex the index of the first element (inclusive) to be included in the calculation
     * @param toIndex the index of the last element (exclusive) to be included in the calculation
     * @return an OptionalChar containing the median element in the specified range,
     *         or an empty OptionalChar if the range is empty
     * @throws IndexOutOfBoundsException if the range is out of bounds
     */
    public OptionalChar median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalChar.empty() : OptionalChar.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     * Performs the given action for each element of this list.
     * 
     * @param action the action to be performed for each element
     */
    public void forEach(final CharConsumer action) {
        forEach(0, size, action);
    }

    /**
     * Performs the given action for each element in the specified range of this list.
     * The range extends from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     * If {@code fromIndex > toIndex}, the elements are processed in reverse order.
     * 
     * @param fromIndex the index of the first element (inclusive) to process
     * @param toIndex the index after the last element (exclusive) to process,
     *                or -1 to process in reverse from {@code fromIndex} to the beginning
     * @param action the action to be performed for each element
     * @throws IndexOutOfBoundsException if the range is out of bounds
     */
    public void forEach(final int fromIndex, final int toIndex, final CharConsumer action) throws IndexOutOfBoundsException {
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
     * Returns the first element in this list wrapped in an OptionalChar.
     * 
     * @return an OptionalChar containing the first element, or an empty OptionalChar if this list is empty
     */
    public OptionalChar first() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(elementData[0]);
    }

    /**
     * Returns the last element in this list wrapped in an OptionalChar.
     * 
     * @return an OptionalChar containing the last element, or an empty OptionalChar if this list is empty
     */
    public OptionalChar last() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(elementData[size() - 1]);
    }

    /**
     * Returns a new CharList containing only the distinct elements from the specified range of this list.
     * The order of elements is preserved (keeps the first occurrence of each element).
     * 
     * @param fromIndex the index of the first element (inclusive) to include
     * @param toIndex the index after the last element (exclusive) to include
     * @return a new CharList containing distinct elements from the specified range
     * @throws IndexOutOfBoundsException if the range is out of bounds
     */
    @Override
    public CharList distinct(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
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
     * <p>This method iterates through all elements in the list and determines if any char value
     * appears more than once. The comparison is done using char equality (==).</p>
     * 
     * <p>Performance: O(n²) in the worst case for unsorted lists, where n is the size of the list.</p>
     *
     * @return {@code true} if the list contains at least one duplicate element, {@code false} otherwise.
     *         Returns {@code false} for empty lists.
     */
    @Override
    public boolean hasDuplicates() {
        return N.hasDuplicates(elementData, 0, size, false);
    }

    /**
     * Checks whether the elements in this list are sorted in ascending order.
     * 
     * <p>This method verifies if each element is less than or equal to the next element
     * in the list. For char values, this means checking if they are in ascending order
     * according to their Unicode values.</p>
     * 
     * <p>Performance: O(n) where n is the size of the list.</p>
     *
     * @return {@code true} if the list is sorted in ascending order, {@code false} otherwise.
     *         Returns {@code true} for empty lists and lists with a single element.
     */
    @Override
    public boolean isSorted() {
        return N.isSorted(elementData, 0, size);
    }

    /**
     * Sorts the elements of this list in ascending order.
     * 
     * <p>This method modifies the list in-place, arranging all char elements from lowest
     * to highest based on their Unicode values. The sorting algorithm used provides
     * O(n log n) performance on average.</p>
     * 
     * <p>After this operation, {@code isSorted()} will return {@code true}.</p>
     * 
     * <p>Note: This method does nothing if the list has fewer than 2 elements.</p>
     */
    @Override
    public void sort() {
        if (size > 1) {
            N.sort(elementData, 0, size);
        }
    }

    /**
     * Sorts the elements of this list in ascending order using a parallel sorting algorithm.
     * 
     * <p>This method leverages multiple threads to sort the list more efficiently on multi-core
     * systems. It's particularly beneficial for large lists where the overhead of parallelization
     * is offset by the performance gains. The elements are sorted based on their Unicode values.</p>
     * 
     * <p>For small lists, this may be slower than {@link #sort()} due to thread overhead.</p>
     * 
     * <p>After this operation, {@code isSorted()} will return {@code true}.</p>
     * 
     * <p>Note: This method does nothing if the list has fewer than 2 elements.</p>
     */
    public void parallelSort() {
        if (size > 1) {
            N.parallelSort(elementData, 0, size);
        }
    }

    /**
     * Sorts the elements of this list in descending order.
     * 
     * <p>This method first sorts the list in ascending order, then reverses it to achieve
     * descending order. All char elements are arranged from highest to lowest based on
     * their Unicode values.</p>
     * 
     * <p>Performance: O(n log n) for sorting plus O(n) for reversing.</p>
     * 
     * <p>Note: This method does nothing if the list has fewer than 2 elements.</p>
     */
    @Override
    public void reverseSort() {
        if (size > 1) {
            sort();
            reverse();
        }
    }

    /**
     * Searches for the specified char value in this sorted list using binary search algorithm.
     * 
     * <p><b>Important:</b> This list must be sorted in ascending order before calling this method.
     * If the list is not sorted, the results are undefined and may be incorrect.</p>
     * 
     * <p>Performance: O(log n) where n is the size of the list.</p>
     *
     * @param valueToFind the char value to search for
     * @return the index of the search key if it is contained in the list;
     *         otherwise, {@code (-(insertion point) - 1)}. The insertion point is defined as
     *         the point at which the key would be inserted into the list: the index of the
     *         first element greater than the key, or {@code size()} if all elements in the
     *         list are less than the specified key.
     */
    public int binarySearch(final char valueToFind) {
        return N.binarySearch(elementData, valueToFind);
    }

    /**
     * Searches for the specified char value within a range of this sorted list using binary search.
     * 
     * <p><b>Important:</b> The specified range must be sorted in ascending order before calling
     * this method. If the range is not sorted, the results are undefined and may be incorrect.</p>
     * 
     * <p>Performance: O(log(toIndex - fromIndex))</p>
     *
     * @param fromIndex the index of the first element (inclusive) to be searched
     * @param toIndex the index of the last element (exclusive) to be searched
     * @param valueToFind the char value to search for
     * @return the index of the search key if it is contained in the specified range;
     *         otherwise, {@code (-(insertion point) - 1)}. The insertion point is defined as
     *         the point at which the key would be inserted into the range: the index of the
     *         first element in the range greater than the key, or {@code toIndex} if all
     *         elements in the range are less than the specified key.
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    public int binarySearch(final int fromIndex, final int toIndex, final char valueToFind) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return N.binarySearch(elementData, fromIndex, toIndex, valueToFind);
    }

    /**
     * Reverses the order of all elements in this list.
     * 
     * <p>This method modifies the list in-place, so the first element becomes the last,
     * the second element becomes the second-to-last, and so on. This operation is
     * performed in linear time.</p>
     * 
     * <p>Performance: O(n/2) where n is the size of the list.</p>
     * 
     * <p>Note: This method does nothing if the list has fewer than 2 elements.</p>
     */
    @Override
    public void reverse() {
        if (size > 1) {
            N.reverse(elementData, 0, size);
        }
    }

    /**
     * Reverses the order of elements in the specified range within this list.
     * 
     * <p>This method modifies the list in-place, reversing only the elements between
     * {@code fromIndex} (inclusive) and {@code toIndex} (exclusive). Elements outside
     * this range remain unchanged.</p>
     * 
     * <p>Performance: O((toIndex - fromIndex)/2)</p>
     * 
     * <p>Note: This method does nothing if the range contains fewer than 2 elements.</p>
     *
     * @param fromIndex the index of the first element (inclusive) to be reversed
     * @param toIndex the index after the last element (exclusive) to be reversed
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
     * Randomly shuffles the elements in this list.
     * 
     * <p>This method uses a default random number generator to randomly permute the list
     * elements. Each possible permutation has equal likelihood. After this operation,
     * the elements will be in a random order.</p>
     * 
     * <p>This implementation uses the Fisher-Yates shuffle algorithm, which runs in O(n) time.</p>
     * 
     * <p>Note: This method does nothing if the list has fewer than 2 elements.</p>
     */
    @Override
    public void shuffle() {
        if (size() > 1) {
            N.shuffle(elementData, 0, size);
        }
    }

    /**
     * Randomly shuffles the elements in this list using the specified random number generator.
     * 
     * <p>This method uses the provided Random object to randomly permute the list elements.
     * This is useful when you need reproducible shuffling (by using a Random with a specific
     * seed) or when you need a cryptographically strong random number generator.</p>
     * 
     * <p>This implementation uses the Fisher-Yates shuffle algorithm, which runs in O(n) time.</p>
     * 
     * <p>Note: This method does nothing if the list has fewer than 2 elements.</p>
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
     * <p>This method exchanges the elements at indices {@code i} and {@code j}. If {@code i}
     * and {@code j} are equal, invoking this method leaves the list unchanged.</p>
     * 
     * <p>Performance: O(1)</p>
     *
     * @param i the index of the first element to swap
     * @param j the index of the second element to swap
     * @throws IndexOutOfBoundsException if either {@code i} or {@code j} is out of range
     *         (i.e., {@code i < 0 || i >= size()} or {@code j < 0 || j >= size()})
     */
    @Override
    public void swap(final int i, final int j) {
        rangeCheck(i);
        rangeCheck(j);

        set(i, set(j, elementData[i]));
    }

    /**
     * Returns a copy of this list.
     * 
     * <p>This method creates a new CharList instance containing all elements from this list
     * in the same order. The returned list is independent of this list; changes to either
     * list will not affect the other.</p>
     * 
     * <p>Performance: O(n) where n is the size of the list.</p>
     *
     * @return a new CharList containing all elements from this list
     */
    @Override
    public CharList copy() {
        return new CharList(N.copyOfRange(elementData, 0, size));
    }

    /**
     * Returns a copy of the specified range of this list.
     * 
     * <p>This method creates a new CharList containing the elements from index {@code fromIndex}
     * (inclusive) to index {@code toIndex} (exclusive). The returned list is independent of
     * this list; changes to either list will not affect the other.</p>
     * 
     * <p>Performance: O(toIndex - fromIndex)</p>
     *
     * @param fromIndex the index of the first element (inclusive) to be copied
     * @param toIndex the index after the last element (exclusive) to be copied
     * @return a new CharList containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public CharList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new CharList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     * Returns a copy of elements from this list with the specified step.
     * 
     * <p>This method creates a new CharList by selecting elements at regular intervals
     * defined by the step parameter. Starting from {@code fromIndex}, it includes every
     * {@code step}-th element until reaching {@code toIndex}.</p>
     * 
     * <p>Special cases:</p>
     * <ul>
     * <li>If {@code step > 0}: iterates forward from fromIndex to toIndex</li>
     * <li>If {@code step < 0}: iterates backward from fromIndex to toIndex</li>
     * <li>If {@code fromIndex > toIndex} and {@code step < 0}: creates a reversed copy</li>
     * </ul>
     * 
     * <p>Examples:</p>
     * <ul>
     * <li>{@code copy(0, 10, 2)} returns elements at indices 0, 2, 4, 6, 8</li>
     * <li>{@code copy(9, -1, -2)} returns elements at indices 9, 7, 5, 3, 1</li>
     * </ul>
     *
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive for positive step, inclusive for negative step)
     * @param step the step size between selected elements
     * @return a new CharList containing the selected elements
     * @throws IndexOutOfBoundsException if the indices are out of range
     * @throws IllegalArgumentException if {@code step} is 0
     * @see N#copyOfRange(int[], int, int, int)
     */
    @Override
    public CharList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex));

        return new CharList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Splits this list into multiple sublists of the specified size.
     * 
     * <p>This method divides the specified range of the list into consecutive sublists,
     * each containing up to {@code chunkSize} elements. The last sublist may contain fewer
     * elements if the range size is not evenly divisible by {@code chunkSize}.</p>
     * 
     * <p>Each returned sublist is a new independent CharList instance. Modifications to
     * the returned sublists do not affect this list or each other.</p>
     * 
     * <p>Example: Splitting [1,2,3,4,5,6,7,8,9] with chunkSize 3 returns [[1,2,3], [4,5,6], [7,8,9]]</p>
     *
     * @param fromIndex the index of the first element (inclusive) to be included
     * @param toIndex the index after the last element (exclusive) to be included
     * @param chunkSize the desired size of each sublist (must be positive)
     * @return a List containing the sublists, each of type CharList
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     * @throws IllegalArgumentException if {@code chunkSize <= 0}
     */
    @Override
    public List<CharList> split(final int fromIndex, final int toIndex, final int chunkSize) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<char[]> list = N.split(elementData, fromIndex, toIndex, chunkSize);
        @SuppressWarnings("rawtypes")
        final List<CharList> result = (List) list;

        for (int i = 0, len = list.size(); i < len; i++) {
            result.set(i, of(list.get(i)));
        }

        return result;
    }

    //    @Override
    //    public List<CharList> split(int fromIndex, int toIndex, CharPredicate predicate) {
    //        checkIndex(fromIndex, toIndex);
    //
    //        final List<CharList> result = new ArrayList<>();
    //        CharList piece = null;
    //
    //        for (int i = fromIndex; i < toIndex;) {
    //            if (piece == null) {
    //                piece = CharList.of(N.EMPTY_CHAR_ARRAY);
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
     * Trims the capacity of this list to be the list's current size.
     * 
     * <p>This method minimizes the storage of a CharList instance. If the internal array
     * has excess capacity (i.e., more space than needed for the current elements), this
     * method creates a new array with the exact size needed and copies the elements to it.</p>
     * 
     * <p>This operation can be useful to minimize memory usage after removing many elements
     * from a list or when a list's size has stabilized.</p>
     * 
     * <p>Performance: O(n) if trimming is needed, O(1) if the capacity already matches the size.</p>
     *
     * @return this CharList instance (for method chaining)
     */
    @Override
    public CharList trimToSize() {
        if (elementData.length > size) {
            elementData = N.copyOfRange(elementData, 0, size);
        }

        return this;
    }

    /**
     * Removes all elements from this list.
     * 
     * <p>After this call returns, the list will be empty. The capacity of the list is
     * unchanged; to also minimize memory usage, call {@link #trimToSize()} after clearing.</p>
     * 
     * <p>This method also clears the underlying array to help garbage collection.</p>
     * 
     * <p>Performance: O(n) where n is the size of the list.</p>
     */
    @Override
    public void clear() {
        if (size > 0) {
            N.fill(elementData, 0, size, (char) 0);
        }

        size = 0;
    }

    /**
     * Returns {@code true} if this list contains no elements.
     * 
     * <p>This is a convenience method equivalent to checking if {@code size() == 0}.</p>
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
     * <p>This method returns the count of actual elements stored in the list, not the
     * capacity of the underlying array.</p>
     *
     * @return the number of elements in this list
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns a List containing all elements in this list boxed as Character objects.
     * 
     * <p>This method creates a new ArrayList and boxes each primitive char value into
     * a Character object. This is useful when you need to work with Java Collections
     * Framework or other APIs that require object types.</p>
     * 
     * <p>Performance: O(n) where n is the size of the list. Note that boxing operations
     * add overhead compared to working with primitive values.</p>
     *
     * @return a new List<Character> containing all elements from this list
     */
    @Override
    public List<Character> boxed() {
        return boxed(0, size);
    }

    /**
     * Returns a List containing elements in the specified range boxed as Character objects.
     * 
     * <p>This method creates a new ArrayList and boxes each primitive char value in the
     * specified range into a Character object. The range is defined from {@code fromIndex}
     * (inclusive) to {@code toIndex} (exclusive).</p>
     * 
     * <p>Performance: O(toIndex - fromIndex). Note that boxing operations add overhead
     * compared to working with primitive values.</p>
     *
     * @param fromIndex the index of the first element (inclusive) to be boxed
     * @param toIndex the index after the last element (exclusive) to be boxed
     * @return a new List<Character> containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public List<Character> boxed(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Character> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    /**
     * Returns a new array containing all elements in this list.
     * 
     * <p>This method creates a new char array and copies all elements from this list into it.
     * The returned array will have a length equal to the size of this list. Modifications to
     * the returned array will not affect this list.</p>
     * 
     * <p>Performance: O(n) where n is the size of the list.</p>
     *
     * @return a new char array containing all elements from this list in proper sequence
     */
    @Override
    public char[] toArray() {
        return N.copyOfRange(elementData, 0, size);
    }

    /**
     * Converts this CharList to an IntList.
     * 
     * <p>This method creates a new IntList where each char element is widened to an int value.
     * The conversion preserves the numeric value of each character (its Unicode code point).
     * This is useful when you need to perform integer arithmetic on character values.</p>
     * 
     * <p>Performance: O(n) where n is the size of the list.</p>
     *
     * @return a new IntList containing the widened values of all elements in this list
     */
    public IntList toIntList() {
        final int[] a = new int[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i];//NOSONAR
        }

        return IntList.of(a);
    }

    /**
     * Returns a Collection containing the elements from the specified range converted to their boxed type.
     * The type of Collection returned is determined by the provided supplier function.
     * The returned collection is independent of this list.
     *
     * @param <C> the type of the collection to return
     * @param fromIndex the index of the first element (inclusive) to include
     * @param toIndex the index after the last element (exclusive) to include
     * @param supplier a function which produces a new collection of the desired type
     * @return a collection containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public <C extends Collection<Character>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
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
     * @param fromIndex the index of the first element (inclusive) to include
     * @param toIndex the index after the last element (exclusive) to include
     * @param supplier a function which produces a new Multiset of the desired type
     * @return a Multiset containing the specified range of elements with their counts
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    @Override
    public Multiset<Character> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Character>> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Character> multiset = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            multiset.add(elementData[i]);
        }

        return multiset;
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     * 
     * <p>The returned iterator is a specialized primitive iterator that avoids boxing
     * overhead. It provides methods like {@code nextChar()} to retrieve primitive values
     * directly.</p>
     * 
     * <p>The iterator is fail-fast: if the list is structurally modified after the iterator
     * is created, it will throw a ConcurrentModificationException on the next access.</p>
     *
     * @return a CharIterator over the elements in this list
     */
    @Override
    public CharIterator iterator() {
        if (isEmpty()) {
            return CharIterator.EMPTY;
        }

        return CharIterator.of(elementData, 0, size);
    }

    /**
     * Returns a CharStream with this list as its source.
     * 
     * <p>This method creates a stream that can be used to perform functional-style operations
     * on the elements of this list. The stream operates on primitive char values, avoiding
     * boxing overhead.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * charList.stream()
     *     .filter(ch -> Character.isLetter(ch))
     *     .map(ch -> Character.toUpperCase(ch))
     *     .forEach(System.out::print);
     * }</pre>
     *
     * @return a CharStream over all elements in this list
     */
    public CharStream stream() {
        return CharStream.of(elementData, 0, size());
    }

    /**
     * Returns a CharStream over the specified range of this list.
     * 
     * <p>This method creates a stream that operates on elements from {@code fromIndex}
     * (inclusive) to {@code toIndex} (exclusive). The stream operates on primitive char
     * values, avoiding boxing overhead.</p>
     *
     * @param fromIndex the index of the first element (inclusive) to include in the stream
     * @param toIndex the index after the last element (exclusive) to include in the stream
     * @return a CharStream over the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
     *         or {@code fromIndex > toIndex}
     */
    public CharStream stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return CharStream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in this list.
     * 
     * <p>This method provides constant-time access to the first element without removing it
     * from the list.</p>
     *
     * @return the first char value in the list
     * @throws NoSuchElementException if the list is empty
     */
    public char getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in this list.
     * 
     * <p>This method provides constant-time access to the last element without removing it
     * from the list.</p>
     *
     * @return the last char value in the list
     * @throws NoSuchElementException if the list is empty
     */
    public char getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list.
     * 
     * <p>This method shifts all existing elements one position to the right (increasing their
     * indices by 1) and inserts the new element at index 0. This operation has O(n) time
     * complexity where n is the size of the list.</p>
     * 
     * <p>If the list's capacity needs to be increased, it will be grown automatically.</p>
     *
     * @param e the char element to add at the beginning of the list
     */
    public void addFirst(final char e) {
        add(0, e);
    }

    /**
     * Appends the specified element to the end of this list.
     * 
     * <p>This method adds the element after the current last element. This operation has
     * amortized O(1) time complexity. If the list's capacity needs to be increased, it will
     * be grown automatically.</p>
     * 
     * <p>This method is equivalent to {@code add(e)}.</p>
     *
     * @param e the char element to append to the list
     */
    public void addLast(final char e) {
        add(size, e);
    }

    /**
     * Removes and returns the first element from this list.
     * 
     * <p>This method removes the element at index 0 and shifts all remaining elements one
     * position to the left (decreasing their indices by 1). This operation has O(n) time
     * complexity where n is the size of the list.</p>
     *
     * @return the first char value that was removed from the list
     * @throws NoSuchElementException if the list is empty
     */
    public char removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(0);
    }

    /**
     * Removes and returns the last element from this list.
     * 
     * <p>This method removes the element at the last position. This operation has O(1)
     * time complexity.</p>
     *
     * @return the last char value that was removed from the list
     * @throws NoSuchElementException if the list is empty
     */
    public char removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(size - 1);
    }

    //    /**
    //     * Returns a new CharList with the elements in reverse order.
    //     *
    //     * @return A new CharList with all elements of the current list in reverse order.
    //     */
    //    public CharList reversed() {
    //        final char[] a = N.copyOfRange(elementData, 0, size);
    //
    //        N.reverse(a);
    //
    //        return new CharList(a);
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
    //    public <R, E extends Exception> R apply(final Throwables.Function<? super CharList, ? extends R, E> func) throws E {
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
    //    public <R, E extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super CharList, ? extends R, E> func) throws E {
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
    //    public <E extends Exception> void accept(final Throwables.Consumer<? super CharList, E> action) throws E {
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
    //    public <E extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super CharList, E> action) throws E {
    //        return If.is(size > 0).then(this, action);
    //    }

    /**
     * Returns a hash code value for this list.
     * 
     * <p>The hash code is computed based on the elements in the list and their order.
     * Two CharList objects with the same elements in the same order will have the same
     * hash code. This method is consistent with {@link #equals(Object)}.</p>
     * 
     * <p>The hash code is computed using a algorithm similar to that used by List.hashCode(),
     * adapted for primitive char values.</p>
     *
     * @return the hash code value for this list
     */
    @Override
    public int hashCode() {
        return N.hashCode(elementData, 0, size);
    }

    /**
     * Compares the specified object with this list for equality.
     * 
     * <p>Returns {@code true} if and only if the specified object is also a CharList,
     * both lists have the same size, and all corresponding pairs of elements in the two
     * lists are equal. In other words, two lists are defined to be equal if they contain
     * the same elements in the same order.</p>
     * 
     * <p>This implementation first checks if the specified object is this list. If so,
     * it returns {@code true}. Then, it checks if the specified object is a CharList.
     * If not, it returns {@code false}. Finally, it compares the elements of both lists.</p>
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

        if (obj instanceof CharList other) {
            return size == other.size && N.equals(elementData, 0, other.elementData, 0, size);
        }

        return false;
    }

    /**
     * Returns a string representation of this list.
     * 
     * <p>The string representation consists of the list's elements, enclosed in square
     * brackets ("[]"). Adjacent elements are separated by the characters ", " (comma and space).
     * Elements are displayed as their string representation (the character itself).</p>
     * 
     * <p>Examples:</p>
     * <ul>
     * <li>An empty list: "[]"</li>
     * <li>A list containing 'a', 'b', 'c': "[a, b, c]"</li>
     * </ul>
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
            elementData = new char[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            final int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}

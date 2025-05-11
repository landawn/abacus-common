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
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 *
 */
public final class IntList extends PrimitiveList<Integer, int[], IntList> {

    @Serial
    private static final long serialVersionUID = 8661773953226671696L;

    static final Random RAND = new SecureRandom();

    private int[] elementData = N.EMPTY_INT_ARRAY;

    private int size = 0;

    /**
     * Constructs an empty IntList.
     */
    public IntList() {
    }

    /**
     * Constructs an IntList with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the list
     */
    public IntList(final int initialCapacity) {
        elementData = initialCapacity == 0 ? N.EMPTY_INT_ARRAY : new int[initialCapacity];
    }

    /**
     * Constructs an IntList using the specified array as the element array for this list without copying action.
     *
     * @param a the array to be used as the element array for this list
     */
    public IntList(final int[] a) {
        this(a, a.length);
    }

    /**
     * Constructs an IntList using the specified array as the element array for this list without copying action.
     *
     * @param a the array to be used as the element array for this list
     * @param size the number of elements in the list
     * @throws IndexOutOfBoundsException if the specified size is out of bounds
     */
    public IntList(final int[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates an IntList from the specified array of integers.
     *
     * @param a the array of integers to be used as the element array for this list
     * @return a new IntList containing the elements of the specified array
     */
    public static IntList of(final int... a) {
        return new IntList(N.nullToEmpty(a));
    }

    /**
     * Creates an IntList from the specified array of integers and size.
     *
     * @param a the array of integers to be used as the element array for this list
     * @param size the number of elements in the list
     * @return a new IntList containing the elements of the specified array
     * @throws IndexOutOfBoundsException if the specified size is out of bounds
     */
    public static IntList of(final int[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new IntList(N.nullToEmpty(a), size);
    }

    /**
     * Creates an IntList that is a copy of the specified array.
     *
     * @param a the array to be copied
     * @return a new IntList containing the elements copied from the specified array
     */
    public static IntList copyOf(final int[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates an IntList that is a copy of the specified array within the given range.
     *
     * @param a the array to be copied
     * @param fromIndex the initial index of the range to be copied, inclusive
     * @param toIndex the final index of the range to be copied, exclusive
     * @return a new IntList containing the elements copied from the specified array within the given range
     * @throws IndexOutOfBoundsException if the specified range is out of bounds
     */
    public static IntList copyOf(final int[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates an IntList with elements ranging from startInclusive to endExclusive.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a new IntList containing the elements in the specified range
     */
    public static IntList range(final int startInclusive, final int endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     * Creates an IntList with elements ranging from startInclusive to endExclusive, incremented by the specified step.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param by the step value for incrementing
     * @return a new IntList containing the elements in the specified range with the given step
     */
    public static IntList range(final int startInclusive, final int endExclusive, final int by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     * Creates an IntList with elements ranging from startInclusive to endInclusive.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a new IntList containing the elements in the specified range
     */
    public static IntList rangeClosed(final int startInclusive, final int endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     * Creates an IntList with elements ranging from startInclusive to endInclusive, incremented by the specified step.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @param by the step value for incrementing
     * @return a new IntList containing the elements in the specified range with the given step
     */
    public static IntList rangeClosed(final int startInclusive, final int endInclusive, final int by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     * Creates an IntList with the specified element repeated a given number of times.
     *
     * @param element the int value to be repeated
     * @param len the number of times to repeat the element
     * @return a new IntList containing the repeated elements
     */
    public static IntList repeat(final int element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates an IntList with random int elements.
     *
     * @param len the number of random elements to generate
     * @return a new IntList containing the random elements
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
     * Creates an IntList with random int elements within the specified range.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param len the number of random elements to generate
     * @return a new IntList containing the random elements within the specified range
     * @throws IllegalArgumentException if startInclusive is not less than endExclusive
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
     * Returns the original element array without copying.
     *
     * @return
     */
    @Beta
    @Override
    public int[] array() {
        return elementData;
    }

    /**
     *
     * @param index
     * @return
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
     *
     * @param index
     * @param e
     * @return
     */
    public int set(final int index, final int e) {
        rangeCheck(index);

        final int oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     *
     * @param e
     */
    public void add(final int e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     *
     * @param index
     * @param e
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
     * Adds the all.
     *
     * @param c
     * @return
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
     * Adds the all.
     *
     * @param index
     * @param c
     * @return
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
     * Adds the all.
     *
     * @param a
     * @return
     */
    @Override
    public boolean addAll(final int[] a) {
        return addAll(size(), a);
    }

    /**
     * Adds the all.
     *
     * @param index
     * @param a
     * @return
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
     *
     * @param e
     * @return <tt>true</tt> if this list contained the specified element
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
     * Removes the all occurrences.
     *
     * @param e
     * @return <tt>true</tt> if this list contained the specified element
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
     * Removes the all.
     *
     * @param c
     * @return
     */
    @Override
    public boolean removeAll(final IntList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        return batchRemove(c, false) > 0;
    }

    /**
     * Removes the all.
     *
     * @param a
     * @return
     */
    @Override
    public boolean removeAll(final int[] a) {
        if (N.isEmpty(a)) {
            return false;
        }

        return removeAll(of(a));
    }

    /**
     * Removes the elements which match the given predicate.
     *
     * @param p
     * @return
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
     *
     * @param c
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param index
     * @return
     */
    public int delete(final int index) {
        rangeCheck(index);

        final int oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     *
     * @param indices
     */
    @Override
    public void deleteAllByIndices(final int... indices) {
        if (N.isEmpty(indices)) {
            return;
        }

        final int[] tmp = N.deleteAllByIndices(elementData, indices);
        N.copy(tmp, 0, elementData, 0, tmp.length);
        N.fill(elementData, tmp.length, size, 0);
        size = tmp.length;
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
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
     *
     * @param fromIndex
     * @param toIndex
     * @param newPositionStartIndex
     */
    @Override
    public void moveRange(final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        N.moveRange(elementData, fromIndex, toIndex, newPositionStartIndex);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @throws IndexOutOfBoundsException
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
     *
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @throws IndexOutOfBoundsException
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
     *
     * @param oldVal
     * @param newVal
     * @return
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
     *
     * @param operator
     */
    public void replaceAll(final IntUnaryOperator operator) {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsInt(elementData[i]);
        }
    }

    /**
     *
     * @param predicate
     * @param newValue
     * @return
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
     *
     * @param val
     */
    public void fill(final int val) {
        fill(0, size(), val);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param val
     * @throws IndexOutOfBoundsException
     */
    public void fill(final int fromIndex, final int toIndex, final int val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     *
     * @param valueToFind
     * @return
     */
    public boolean contains(final int valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     *
     * @param c
     * @return
     */
    @Override
    public boolean containsAny(final IntList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    /**
     *
     * @param a
     * @return
     */
    @Override
    public boolean containsAny(final int[] a) {
        if (isEmpty() || N.isEmpty(a)) {
            return false;
        }

        return !disjoint(a);
    }

    /**
     *
     * @param c
     * @return
     */
    @Override
    public boolean containsAll(final IntList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        final boolean isThisContainer = size() >= c.size();
        final IntList container = isThisContainer ? this : c;
        final int[] iterElements = isThisContainer ? c.array() : array();

        if (needToSet(size(), c.size())) {
            final Set<Integer> set = container.toSet();

            for (int i = 0, iterLen = isThisContainer ? c.size() : size(); i < iterLen; i++) {
                if (!set.contains(iterElements[i])) {
                    return false;
                }
            }
        } else {
            for (int i = 0, iterLen = isThisContainer ? c.size() : size(); i < iterLen; i++) {
                if (!container.contains(iterElements[i])) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     *
     * @param a
     * @return
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
     *
     * @param c
     * @return
     */
    @Override
    public boolean disjoint(final IntList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        final boolean isThisContainer = size() >= c.size();
        final IntList container = isThisContainer ? this : c;
        final int[] iterElements = isThisContainer ? c.array() : array();

        if (needToSet(size(), c.size())) {
            final Set<Integer> set = container.toSet();

            for (int i = 0, iterLen = isThisContainer ? c.size() : size(); i < iterLen; i++) {
                if (set.contains(iterElements[i])) {
                    return false;
                }
            }
        } else {
            for (int i = 0, iterLen = isThisContainer ? c.size() : size(); i < iterLen; i++) {
                if (container.contains(iterElements[i])) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     *
     * @param b
     * @return
     */
    @Override
    public boolean disjoint(final int[] b) {
        if (isEmpty() || N.isEmpty(b)) {
            return true;
        }

        return disjoint(of(b));
    }

    /**
     * Returns a new list with all the elements occurred in both {@code a} and {@code b}. Occurrences are considered.
     *
     * <pre>
     * IntList a = IntList.of(0, 1, 2, 2, 3);
     * IntList b = IntList.of(2, 5, 1);
     * a.retainAll(b); // The elements remained in a will be: [1, 2, 2].
     *
     * IntList a = IntList.of(0, 1, 2, 2, 3);
     * IntList b = IntList.of(2, 5, 1);
     * IntList c = a.intersection(b); // The elements c in a will be: [1, 2].
     *
     * IntList a = IntList.of(0, 1, 2, 2, 3);
     * IntList b = IntList.of(2, 5, 1, 2);
     * IntList c = a.intersection(b); // The elements c in a will be: [1, 2, 2].
     * </pre>
     *
     * @param b
     * @return
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
     * Returns a new list with all the elements occurred in both {@code a} and {@code b}. Occurrences are considered.
     *
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    @Override
    public IntList intersection(final int[] b) {
        if (N.isEmpty(b)) {
            return new IntList();
        }

        return intersection(of(b));
    }

    /**
     * Returns a new list with the elements in this list but not in the specified list/array {@code b}. Occurrences are considered.
     *
     * <pre>
     * IntList a = IntList.of(0, 1, 2, 2, 3);
     * IntList b = IntList.of(2, 5, 1);
     * a.removeAll(b); // The elements remained in a will be: [0, 3].
     *
     * IntList a = IntList.of(0, 1, 2, 2, 3);
     * IntList b = IntList.of(2, 5, 1);
     * IntList c = a.difference(b); // The elements c in a will be: [0, 2, 3].
     * </pre>
     *
     * @param b
     * @return
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
     * Returns a new list with the elements in this list but not in the specified list/array {@code b}. Occurrences are considered.
     *
     * @param b
     * @return
     * @see #difference(IntList)
     */
    @Override
    public IntList difference(final int[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(b));
    }

    /**
     * Returns a new list the elements that are in this list but not in the specified list/array and vice versa. Occurrences are considered
     *
     * <pre>
     * IntList a = IntList.of(0, 1, 2, 2, 3);
     * IntList b = IntList.of(2, 5, 1);
     * IntList c = a.symmetricDifference(b); // The elements c in a will be: [0, 2, 3, 5].
     * </pre>
     *
     * @param b
     * @return this.difference(b).addAll(b.difference ( this))
     * @see #difference(IntList)
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
     * Returns a new list the elements that are in this list but not in the specified list/array and vice versa. Occurrences are considered
     *
     * @param b
     * @return a new list the elements that are in this list but not in the specified list/array and vice versa. Occurrences are considered
     * @see #symmetricDifference(IntList)
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
     *
     * @param valueToFind
     * @return
     */
    public int occurrencesOf(final int valueToFind) {
        return N.occurrencesOf(elementData, valueToFind);
    }

    /**
     *
     * @param valueToFind
     * @return
     */
    public int indexOf(final int valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     *
     * @param valueToFind
     * @param fromIndex
     * @return
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
     * Last index of.
     *
     * @param valueToFind
     * @return
     */
    public int lastIndexOf(final int valueToFind) {
        return lastIndexOf(valueToFind, size);
    }

    /**
     * Last index of.
     * @param valueToFind
     * @param startIndexFromBack the start index to traverse backwards from. Inclusive.
     *
     * @return
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

    public OptionalInt min() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(N.min(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalInt min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalInt.empty() : OptionalInt.of(N.min(elementData, fromIndex, toIndex));
    }

    public OptionalInt max() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(N.max(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalInt max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalInt.empty() : OptionalInt.of(N.max(elementData, fromIndex, toIndex));
    }

    public OptionalInt median() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(N.median(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalInt median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalInt.empty() : OptionalInt.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     *
     * @param action
     */
    public void forEach(final IntConsumer action) {
        forEach(0, size, action);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param action
     * @throws IndexOutOfBoundsException
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

    public OptionalInt first() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(elementData[0]);
    }

    public OptionalInt last() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(elementData[size() - 1]);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
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
     * Checks for duplicates.
     *
     * @return
     */
    @Override
    public boolean hasDuplicates() {
        return N.hasDuplicates(elementData, 0, size, false);
    }

    @Override
    public boolean isSorted() {
        return N.isSorted(elementData, 0, size);
    }

    /**
     * Sort.
     */
    @Override
    public void sort() {
        if (size > 1) {
            N.sort(elementData, 0, size);
        }
    }

    /**
     * Parallel sort.
     */
    public void parallelSort() {
        if (size > 1) {
            N.parallelSort(elementData, 0, size);
        }
    }

    /**
     * Reverse sort.
     */
    @Override
    public void reverseSort() {
        if (size > 1) {
            sort();
            reverse();
        }
    }

    /**
     * This List should be sorted first.
     *
     * @param valueToFind
     * @return
     */
    public int binarySearch(final int valueToFind) {
        return N.binarySearch(elementData, valueToFind);
    }

    /**
     * This List should be sorted first.
     *
     * @param fromIndex
     * @param toIndex
     * @param valueToFind
     * @return
     * @throws IndexOutOfBoundsException
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
            N.rotate(elementData, distance);
        }
    }

    /**
     * Shuffle.
     */
    @Override
    public void shuffle() {
        if (size() > 1) {
            N.shuffle(elementData);
        }
    }

    /**
     *
     * @param rnd
     */
    @Override
    public void shuffle(final Random rnd) {
        if (size() > 1) {
            N.shuffle(elementData, rnd);
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

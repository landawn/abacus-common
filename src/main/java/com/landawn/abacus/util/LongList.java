/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.LongStream;

/**
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 *
 */
public final class LongList extends PrimitiveList<Long, long[], LongList> {

    @Serial
    private static final long serialVersionUID = -7764836427712181163L;

    static final Random RAND = new SecureRandom();

    private long[] elementData = N.EMPTY_LONG_ARRAY;

    private int size = 0;

    /**
     * Constructs an empty LongList.
     */
    public LongList() {
    }

    /**
     * Constructs a LongList with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the list
     */
    public LongList(final int initialCapacity) {
        elementData = initialCapacity == 0 ? N.EMPTY_LONG_ARRAY : new long[initialCapacity];
    }

    /**
     * Constructs a LongList using the specified array as the element array for this list without copying action.
     *
     * @param a the array to be used as the element array for this list
     */
    public LongList(final long[] a) {
        this(a, a.length);
    }

    /**
     * Constructs a LongList using the specified array as the element array for this list without copying action.
     *
     * @param a the array to be used as the element array for this list
     * @param size the number of elements in the list
     * @throws IndexOutOfBoundsException if the specified size is out of bounds
     */
    public LongList(final long[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates a LongList from the specified array of longs.
     *
     * @param a the array of longs to be used as the element array for this list
     * @return a new LongList containing the elements of the specified array
     */
    @SafeVarargs
    public static LongList of(final long... a) {
        return new LongList(N.nullToEmpty(a));
    }

    /**
     * Creates a LongList from the specified array of longs and size.
     *
     * @param a the array of longs to be used as the element array for this list
     * @param size the number of elements in the list
     * @return a new LongList containing the elements of the specified array
     * @throws IndexOutOfBoundsException if the specified size is out of bounds
     */
    public static LongList of(final long[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new LongList(N.nullToEmpty(a), size);
    }

    /**
     * Creates a LongList that is a copy of the specified array.
     *
     * @param a the array to be copied
     * @return a new LongList containing the elements copied from the specified array
     */
    public static LongList copyOf(final long[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates a LongList that is a copy of the specified array within the given range.
     *
     * @param a the array to be copied
     * @param fromIndex the initial index of the range to be copied, inclusive
     * @param toIndex the final index of the range to be copied, exclusive
     * @return a new LongList containing the elements copied from the specified array within the given range
     * @throws IndexOutOfBoundsException if the specified range is out of bounds
     */
    public static LongList copyOf(final long[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates a LongList with elements ranging from startInclusive to endExclusive.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a new LongList containing the elements in the specified range
     */
    public static LongList range(final long startInclusive, final long endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     * Creates a LongList with elements ranging from startInclusive to endExclusive, incremented by the specified step.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param by the step value for incrementing
     * @return a new LongList containing the elements in the specified range with the given step
     */
    public static LongList range(final long startInclusive, final long endExclusive, final long by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     * Creates a LongList with elements ranging from startInclusive to endInclusive.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a new LongList containing the elements in the specified range
     */
    public static LongList rangeClosed(final long startInclusive, final long endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     * Creates a LongList with elements ranging from startInclusive to endInclusive, incremented by the specified step.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @param by the step value for incrementing
     * @return a new LongList containing the elements in the specified range with the given step
     */
    public static LongList rangeClosed(final long startInclusive, final long endInclusive, final long by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     * Creates a LongList with the specified element repeated a given number of times.
     *
     * @param element the long value to be repeated
     * @param len the number of times to repeat the element
     * @return a new LongList containing the repeated elements
     */
    public static LongList repeat(final long element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates a LongList with random long elements.
     *
     * @param len the number of random elements to generate
     * @return a new LongList containing the random elements
     */
    public static LongList random(final int len) {
        final long[] a = new long[len];

        for (int i = 0; i < len; i++) {
            a[i] = RAND.nextLong();
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
    public long[] array() {
        return elementData;
    }

    /**
     *
     * @param index
     * @return
     */
    public long get(final int index) {
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
    public long set(final int index, final long e) {
        rangeCheck(index);

        final long oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     *
     * @param e
     */
    public void add(final long e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     *
     * @param index
     * @param e
     */
    public void add(final int index, final long e) {
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
    public boolean addAll(final LongList c) {
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
    public boolean addAll(final int index, final LongList c) {
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
    public boolean addAll(final long[] a) {
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
    public boolean addAll(final int index, final long[] a) {
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
     * Range check for add.
     *
     * @param index
     */
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
    public boolean remove(final long e) {
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
    public boolean removeAllOccurrences(final long e) {
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
    public boolean removeAll(final LongList c) {
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
    public boolean removeAll(final long[] a) {
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
    public boolean removeIf(final LongPredicate p) {
        final LongList tmp = new LongList(size());

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
            final Set<Long> set = N.newLinkedHashSet(size);
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
    public boolean retainAll(final LongList c) {
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
    public boolean retainAll(final long[] a) {
        if (N.isEmpty(a)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(LongList.of(a));
    }

    /**
     *
     * @param c
     * @param complement
     * @return
     */
    private int batchRemove(final LongList c, final boolean complement) {
        final long[] elementData = this.elementData;//NOSONAR

        int w = 0;

        if (c.size() > 3 && size() > 9) {
            final Set<Long> set = c.toSet();

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
    public long delete(final int index) {
        rangeCheck(index);

        final long oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     *
     * @param indices
     */
    @Override
    @SafeVarargs
    public final void deleteAllByIndices(final int... indices) {
        if (N.isEmpty(indices)) {
            return;
        }

        final long[] tmp = N.deleteAllByIndices(elementData, indices);
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
    public void replaceRange(final int fromIndex, final int toIndex, final LongList replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, 0L);
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
    public void replaceRange(final int fromIndex, final int toIndex, final long[] replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, 0L);
        }

        this.size = newSize;
    }

    /**
     *
     * @param oldVal
     * @param newVal
     * @return
     */
    public int replaceAll(final long oldVal, final long newVal) {
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
    public void replaceAll(final LongUnaryOperator operator) {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsLong(elementData[i]);
        }
    }

    /**
     *
     * @param predicate
     * @param newValue
     * @return
     */
    public boolean replaceIf(final LongPredicate predicate, final long newValue) {
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
    public void fill(final long val) {
        fill(0, size(), val);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param val
     * @throws IndexOutOfBoundsException
     */
    public void fill(final int fromIndex, final int toIndex, final long val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     *
     * @param valueToFind
     * @return
     */
    public boolean contains(final long valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     *
     * @param c
     * @return
     */
    @Override
    public boolean containsAny(final LongList c) {
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
    public boolean containsAny(final long[] a) {
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
    public boolean containsAll(final LongList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        final boolean isThisContainer = size() >= c.size();
        final LongList container = isThisContainer ? this : c;
        final long[] iterElements = isThisContainer ? c.array() : array();

        if (needToSet(size(), c.size())) {
            final Set<Long> set = container.toSet();

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
    public boolean containsAll(final long[] a) {
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
    public boolean disjoint(final LongList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        final boolean isThisContainer = size() >= c.size();
        final LongList container = isThisContainer ? this : c;
        final long[] iterElements = isThisContainer ? c.array() : array();

        if (needToSet(size(), c.size())) {
            final Set<Long> set = container.toSet();

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
    public boolean disjoint(final long[] b) {
        if (isEmpty() || N.isEmpty(b)) {
            return true;
        }

        return disjoint(of(b));
    }

    /**
     * Returns a new list with all the elements occurred in both {@code a} and {@code b}. Occurrences are considered.
     *
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    @Override
    public LongList intersection(final LongList b) {
        if (N.isEmpty(b)) {
            return new LongList();
        }

        final Multiset<Long> bOccurrences = b.toMultiset();

        final LongList c = new LongList(N.min(9, size(), b.size()));

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
    public LongList intersection(final long[] b) {
        if (N.isEmpty(b)) {
            return new LongList();
        }

        return intersection(of(b));
    }

    /**
     * Returns a new list with the elements in this list but not in the specified list/array {@code b}. Occurrences are considered.
     *
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    @Override
    public LongList difference(final LongList b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Long> bOccurrences = b.toMultiset();

        final LongList c = new LongList(N.min(size(), N.max(9, size() - b.size())));

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
     * @see IntList#difference(IntList)
     */
    @Override
    public LongList difference(final long[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(b));
    }

    /**
     * Returns a new list the elements that are in this list but not in the specified list/array and vice versa. Occurrences are considered
     *
     * @param b
     * @return a new list the elements that are in this list but not in the specified list/array and vice versa. Occurrences are considered
     * @see IntList#symmetricDifference(IntList)
     */
    @Override
    public LongList symmetricDifference(final LongList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (isEmpty()) {
            return b.copy();
        }

        final Multiset<Long> bOccurrences = b.toMultiset();
        final LongList c = new LongList(N.max(9, Math.abs(size() - b.size())));

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
     * @see IntList#symmetricDifference(IntList)
     */
    @Override
    public LongList symmetricDifference(final long[] b) {
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
    public int occurrencesOf(final long valueToFind) {
        return N.occurrencesOf(elementData, valueToFind);
    }

    /**
     *
     * @param valueToFind
     * @return
     */
    public int indexOf(final long valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     *
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public int indexOf(final long valueToFind, final int fromIndex) {
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
    public int lastIndexOf(final long valueToFind) {
        return lastIndexOf(valueToFind, size);
    }

    /**
     * Last index of.
     * @param valueToFind
     * @param startIndexFromBack the start index to traverse backwards from. Inclusive.
     *
     * @return
     */
    public int lastIndexOf(final long valueToFind, final int startIndexFromBack) {
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

    public OptionalLong min() {
        return size() == 0 ? OptionalLong.empty() : OptionalLong.of(N.min(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalLong min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalLong.empty() : OptionalLong.of(N.min(elementData, fromIndex, toIndex));
    }

    public OptionalLong max() {
        return size() == 0 ? OptionalLong.empty() : OptionalLong.of(N.max(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalLong max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalLong.empty() : OptionalLong.of(N.max(elementData, fromIndex, toIndex));
    }

    public OptionalLong median() {
        return size() == 0 ? OptionalLong.empty() : OptionalLong.of(N.median(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalLong median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalLong.empty() : OptionalLong.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     *
     * @param action
     */
    public void forEach(final LongConsumer action) {
        forEach(0, size, action);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param action
     * @throws IndexOutOfBoundsException
     */
    public void forEach(final int fromIndex, final int toIndex, final LongConsumer action) throws IndexOutOfBoundsException {
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
    //    public <E extends Exception> void forEachIndexed(final Throwables.IntLongConsumer<E> action) throws E {
    //        forEachIndexed(0, size, action);
    //    }
    //
    //    /**
    //     *
    //     * @param <E>
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param action
    //     * @throws IndexOutOfBoundsException
    //     * @throws E the e
    //     */
    //    public <E extends Exception> void forEachIndexed(final int fromIndex, final int toIndex, final Throwables.IntLongConsumer<E> action)
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

    public OptionalLong first() {
        return size() == 0 ? OptionalLong.empty() : OptionalLong.of(elementData[0]);
    }

    public OptionalLong last() {
        return size() == 0 ? OptionalLong.empty() : OptionalLong.of(elementData[size() - 1]);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    @Override
    public LongList distinct(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
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
    public int binarySearch(final long valueToFind) {
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
    public int binarySearch(final int fromIndex, final int toIndex, final long valueToFind) throws IndexOutOfBoundsException {
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
    public LongList copy() {
        return new LongList(N.copyOfRange(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    @Override
    public LongList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new LongList(N.copyOfRange(elementData, fromIndex, toIndex));
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
    public LongList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex));

        return new LongList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Returns List of {@code LongList} with consecutive sub-sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub-sequence (the last may be smaller).
     * @return
     * @throws IndexOutOfBoundsException
     */
    @Override
    public List<LongList> split(final int fromIndex, final int toIndex, final int chunkSize) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<long[]> list = N.split(elementData, fromIndex, toIndex, chunkSize);
        @SuppressWarnings("rawtypes")
        final List<LongList> result = (List) list;

        for (int i = 0, len = list.size(); i < len; i++) {
            result.set(i, of(list.get(i)));
        }

        return result;
    }

    //    @Override
    //    public List<LongList> split(int fromIndex, int toIndex, LongPredicate predicate) {
    //        checkIndex(fromIndex, toIndex);
    //
    //        final List<LongList> result = new ArrayList<>();
    //        LongList piece = null;
    //
    //        for (int i = fromIndex; i < toIndex;) {
    //            if (piece == null) {
    //                piece = LongList.of(N.EMPTY_LONG_ARRAY);
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
    public LongList trimToSize() {
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
    public List<Long> boxed() {
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
    public List<Long> boxed(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Long> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    @Override
    public long[] toArray() {
        return N.copyOfRange(elementData, 0, size);
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
    public <C extends Collection<Long>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
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
    public Multiset<Long> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Long>> supplier) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Long> multiset = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            multiset.add(elementData[i]);
        }

        return multiset;
    }

    @Override
    public LongIterator iterator() {
        if (isEmpty()) {
            return LongIterator.EMPTY;
        }

        return LongIterator.of(elementData, 0, size);
    }

    public LongStream stream() {
        return LongStream.of(elementData, 0, size());
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public LongStream stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return LongStream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in the list.
     *
     * @return The first long value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public long getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in the list.
     *
     * @return The last long value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public long getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list.
     *
     * @param e the element to add
     */
    public void addFirst(final long e) {
        add(0, e);
    }

    /**
     * Inserts the specified element at the end of this list.
     *
     * @param e the element to add
     */
    public void addLast(final long e) {
        add(size, e);
    }

    /**
     * Removes and returns the first element from this list.
     *
     * @return The first long value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public long removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(0);
    }

    /**
     * Removes and returns the last element from this list.
     *
     * @return The last long value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public long removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(size - 1);
    }

    //    /**
    //     * Returns a new LongList with the elements in reverse order.
    //     *
    //     * @return A new LongList with all elements of the current list in reverse order.
    //     */
    //    public LongList reversed() {
    //        final long[] a = N.copyOfRange(elementData, 0, size);
    //
    //        N.reverse(a);
    //
    //        return new LongList(a);
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
    //    public <R, E extends Exception> R apply(final Throwables.Function<? super LongList, ? extends R, E> func) throws E {
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
    //    public <R, E extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super LongList, ? extends R, E> func) throws E {
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
    //    public <E extends Exception> void accept(final Throwables.Consumer<? super LongList, E> action) throws E {
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
    //    public <E extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super LongList, E> action) throws E {
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

        if (obj instanceof final LongList other) {
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
            elementData = new long[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            final int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}

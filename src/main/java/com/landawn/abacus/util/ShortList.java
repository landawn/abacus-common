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
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortUnaryOperator;
import com.landawn.abacus.util.stream.ShortStream;

/**
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 *
 */
public final class ShortList extends PrimitiveList<Short, short[], ShortList> {

    @Serial
    private static final long serialVersionUID = 25682021483156507L;

    static final Random RAND = new SecureRandom();

    private short[] elementData = N.EMPTY_SHORT_ARRAY;

    private int size = 0;

    /**
     * Constructs an empty ShortList.
     */
    public ShortList() {
    }

    /**
     * Constructs a ShortList with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the list
     */
    public ShortList(final int initialCapacity) {
        elementData = initialCapacity == 0 ? N.EMPTY_SHORT_ARRAY : new short[initialCapacity];
    }

    /**
     * Constructs a ShortList using the specified array as the element array for this list without copying action.
     *
     * @param a the array to be used as the element array for this list
     */
    public ShortList(final short[] a) {
        this(N.requireNonNull(a), a.length);
    }

    /**
     * Constructs a ShortList using the specified array as the element array for this list without copying action.
     *
     * @param a the array to be used as the element array for this list
     * @param size the number of elements in the list
     * @throws IndexOutOfBoundsException if the specified size is out of bounds
     */
    public ShortList(final short[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates a ShortList from the specified array of shorts.
     *
     * @param a the array of shorts to be used as the element array for this list
     * @return a new ShortList containing the elements of the specified array
     */
    public static ShortList of(final short... a) {
        return new ShortList(N.nullToEmpty(a));
    }

    /**
     * Creates a ShortList from the specified array of shorts and size.
     *
     * @param a the array of shorts to be used as the element array for this list
     * @param size the number of elements in the list
     * @return a new ShortList containing the elements of the specified array
     * @throws IndexOutOfBoundsException if the specified size is out of bounds
     */
    public static ShortList of(final short[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new ShortList(N.nullToEmpty(a), size);
    }

    /**
     * Creates a ShortList that is a copy of the specified array.
     *
     * @param a the array to be copied
     * @return a new ShortList containing the elements copied from the specified array
     */
    public static ShortList copyOf(final short[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates a ShortList that is a copy of the specified array within the given range.
     *
     * @param a the array to be copied
     * @param fromIndex the initial index of the range to be copied, inclusive
     * @param toIndex the final index of the range to be copied, exclusive
     * @return a new ShortList containing the elements copied from the specified array within the given range
     * @throws IndexOutOfBoundsException if the specified range is out of bounds
     */
    public static ShortList copyOf(final short[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates a ShortList with elements ranging from startInclusive to endExclusive.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a new ShortList containing the elements in the specified range
     */
    public static ShortList range(final short startInclusive, final short endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     * Creates a ShortList with elements ranging from startInclusive to endExclusive, incremented by the specified step.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param by the step value for incrementing
     * @return a new ShortList containing the elements in the specified range with the given step
     */
    public static ShortList range(final short startInclusive, final short endExclusive, final short by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     * Creates a ShortList with elements ranging from startInclusive to endInclusive.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a new ShortList containing the elements in the specified range
     */
    public static ShortList rangeClosed(final short startInclusive, final short endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     * Creates a ShortList with elements ranging from startInclusive to endInclusive, incremented by the specified step.
     *
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @param by the step value for incrementing
     * @return a new ShortList containing the elements in the specified range with the given step
     */
    public static ShortList rangeClosed(final short startInclusive, final short endInclusive, final short by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     * Creates a ShortList with the specified element repeated a given number of times.
     *
     * @param element the short value to be repeated
     * @param len the number of times to repeat the element
     * @return a new ShortList containing the repeated elements
     */
    public static ShortList repeat(final short element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates a ShortList with random short elements.
     *
     * @param len the number of random elements to generate
     * @return a new ShortList containing the random elements
     */
    public static ShortList random(final int len) {
        final int bound = Short.MAX_VALUE - Short.MIN_VALUE + 1;
        final short[] a = new short[len];

        for (int i = 0; i < len; i++) {
            a[i] = (short) (RAND.nextInt(bound) + Short.MIN_VALUE);
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
    public short[] array() {
        return elementData;
    }

    /**
     *
     * @param index
     * @return
     */
    public short get(final int index) {
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
    public short set(final int index, final short e) {
        rangeCheck(index);

        final short oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     *
     * @param e
     */
    public void add(final short e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     *
     * @param index
     * @param e
     */
    public void add(final int index, final short e) {
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
    public boolean addAll(final ShortList c) {
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
    public boolean addAll(final int index, final ShortList c) {
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
    public boolean addAll(final short[] a) {
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
    public boolean addAll(final int index, final short[] a) {
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
    public boolean remove(final short e) {
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
    public boolean removeAllOccurrences(final short e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] != e) {
                elementData[w++] = elementData[i];
            }
        }

        final int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, (short) 0);

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
    public boolean removeAll(final ShortList c) {
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
    public boolean removeAll(final short[] a) {
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
    public boolean removeIf(final ShortPredicate p) {
        final ShortList tmp = new ShortList(size());

        for (int i = 0; i < size; i++) {
            if (!p.test(elementData[i])) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, elementData, 0, tmp.size());
        N.fill(elementData, tmp.size(), size, (short) 0);
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
            final Set<Short> set = N.newLinkedHashSet(size);
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
            N.fill(elementData, idx + 1, size, (short) 0);

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
    public boolean retainAll(final ShortList c) {
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
    public boolean retainAll(final short[] a) {
        if (N.isEmpty(a)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(ShortList.of(a));
    }

    /**
     *
     * @param c
     * @param complement
     * @return
     */
    private int batchRemove(final ShortList c, final boolean complement) {
        final short[] elementData = this.elementData;//NOSONAR

        int w = 0;

        if (c.size() > 3 && size() > 9) {
            final Set<Short> set = c.toSet();

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
            N.fill(elementData, w, size, (short) 0);

            size = w;
        }

        return numRemoved;
    }

    /**
     *
     * @param index
     * @return
     */
    public short delete(final int index) {
        rangeCheck(index);

        final short oldValue = elementData[index];

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

        final short[] tmp = N.deleteAllByIndices(elementData, indices);

        N.copy(tmp, 0, elementData, 0, tmp.length);

        if (size > tmp.length) {
            N.fill(elementData, tmp.length, size, (short) 0);
        }

        size -= elementData.length - tmp.length;
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

        N.fill(elementData, newSize, size, (short) 0);

        this.size = newSize;
    }

    /**
     * Moves a range of elements in this list to a new position within the list.
     * The new position specified by {@code newPositionStartIndexAfterMove} is the start index of the specified range after the move operation, not before the move operation.
     * <br />
     * No elements are deleted in the process, this list maintains its size.
     *
     * @param fromIndex the starting index (inclusive) of the range to be moved
     * @param toIndex the ending index (exclusive) of the range to be moved
     * @param newPositionStartIndexAfterMove the start index of the specified range after the move operation, not before the move operation. 
     *          It must in the range: [0, array.length - (toIndex - fromIndex)]
     * @throws IndexOutOfBoundsException if the range is out of the list bounds or newPositionStartIndexAfterMove is invalid
     */
    @Override
    public void moveRange(final int fromIndex, final int toIndex, final int newPositionStartIndexAfterMove) {
        N.moveRange(elementData, fromIndex, toIndex, newPositionStartIndexAfterMove);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @throws IndexOutOfBoundsException
     */
    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final ShortList replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, (short) 0);
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
    public void replaceRange(final int fromIndex, final int toIndex, final short[] replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, (short) 0);
        }

        this.size = newSize;
    }

    /**
     *
     * @param oldVal
     * @param newVal
     * @return
     */
    public int replaceAll(final short oldVal, final short newVal) {
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
    public void replaceAll(final ShortUnaryOperator operator) {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsShort(elementData[i]);
        }
    }

    /**
     *
     * @param predicate
     * @param newValue
     * @return
     */
    public boolean replaceIf(final ShortPredicate predicate, final short newValue) {
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
    public void fill(final short val) {
        fill(0, size(), val);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param val
     * @throws IndexOutOfBoundsException
     */
    public void fill(final int fromIndex, final int toIndex, final short val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     *
     * @param valueToFind
     * @return
     */
    public boolean contains(final short valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     *
     * @param c
     * @return
     */
    @Override
    public boolean containsAny(final ShortList c) {
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
    public boolean containsAny(final short[] a) {
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
    public boolean containsAll(final ShortList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        if (needToSet(size(), c.size())) {
            final Set<Short> set = this.toSet();

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
     *
     * @param a
     * @return
     */
    @Override
    public boolean containsAll(final short[] a) {
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
    public boolean disjoint(final ShortList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        if (needToSet(size(), c.size())) {
            final Set<Short> set = this.toSet();

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
     *
     * @param b
     * @return
     */
    @Override
    public boolean disjoint(final short[] b) {
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
     * ShortList list1 = ShortList.of((short)0, (short)1, (short)2, (short)2, (short)3);
     * ShortList list2 = ShortList.of((short)1, (short)2, (short)2, (short)4);
     * ShortList result = list1.intersection(list2); // result will be [(short)1, (short)2, (short)2]
     * // One occurrence of '1' (minimum count in both lists) and two occurrences of '2'
     *
     * ShortList list3 = ShortList.of((short)5, (short)5, (short)6);
     * ShortList list4 = ShortList.of((short)5, (short)7);
     * ShortList result2 = list3.intersection(list4); // result will be [(short)5]
     * // One occurrence of '5' (minimum count in both lists)
     * </pre>
     *
     * @param b the list to find common elements with this list
     * @return a new ShortList containing elements present in both this list and the specified list,
     *         considering the minimum number of occurrences in either list.
     *         Returns an empty list if either list is {@code null} or empty.
     * @see #intersection(short[])
     * @see #difference(ShortList)
     * @see #symmetricDifference(ShortList)
     * @see N#intersection(short[], short[])
     * @see N#intersection(int[], int[])
     */
    @Override
    public ShortList intersection(final ShortList b) {
        if (N.isEmpty(b)) {
            return new ShortList();
        }

        final Multiset<Short> bOccurrences = b.toMultiset();

        final ShortList c = new ShortList(N.min(9, size(), b.size()));

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
     * ShortList list1 = ShortList.of((short)0, (short)1, (short)2, (short)2, (short)3);
     * short[] array = new short[]{(short)1, (short)2, (short)2, (short)4};
     * ShortList result = list1.intersection(array); // result will be [(short)1, (short)2, (short)2]
     * // One occurrence of '1' (minimum count in both sources) and two occurrences of '2'
     *
     * ShortList list2 = ShortList.of((short)5, (short)5, (short)6);
     * short[] array2 = new short[]{(short)5, (short)7};
     * ShortList result2 = list2.intersection(array2); // result will be [(short)5]
     * // One occurrence of '5' (minimum count in both sources)
     * </pre>
     *
     * @param b the array to find common elements with this list
     * @return a new ShortList containing elements present in both this list and the specified array,
     *         considering the minimum number of occurrences in either source.
     *         Returns an empty list if the array is {@code null} or empty.
     * @see #intersection(ShortList)
     * @see #difference(short[])
     * @see #symmetricDifference(short[])
     * @see N#intersection(short[], short[])
     * @see N#intersection(int[], int[])
     */
    @Override
    public ShortList intersection(final short[] b) {
        if (N.isEmpty(b)) {
            return new ShortList();
        }

        return intersection(of(b));
    }

    /**
     * Returns a new list with the elements in this list but not in the specified list {@code b},
     * considering the number of occurrences of each element.
     *
     * <p>Example:
     * <pre>
     * ShortList list1 = ShortList.of((short)1, (short)1, (short)2, (short)3);
     * ShortList list2 = ShortList.of((short)1, (short)4);
     * ShortList result = list1.difference(list2); // result will be [(short)1, (short)2, (short)3]
     * // One '1' remains because list1 has two occurrences and list2 has one
     *
     * ShortList list3 = ShortList.of((short)5, (short)6);
     * ShortList list4 = ShortList.of((short)5, (short)5, (short)6);
     * ShortList result2 = list3.difference(list4); // result will be [] (empty)
     * // No elements remain because list4 has at least as many occurrences of each value as list3
     * </pre>
     *
     * @param b the list to compare against this list
     * @return a new ShortList containing the elements that are present in this list but not in the specified list,
     *         considering the number of occurrences.
     * @see #difference(short[])
     * @see #symmetricDifference(ShortList)
     * @see #intersection(ShortList)
     * @see N#difference(short[], short[])
     * @see N#difference(int[], int[])
     */
    @Override
    public ShortList difference(final ShortList b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Short> bOccurrences = b.toMultiset();

        final ShortList c = new ShortList(N.min(size(), N.max(9, size() - b.size())));

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
     * ShortList list1 = ShortList.of((short)1, (short)1, (short)2, (short)3);
     * short[] array = new short[]{(short)1, (short)4};
     * ShortList result = list1.difference(array); // result will be [(short)1, (short)2, (short)3]
     * // One '1' remains because list1 has two occurrences and array has one
     *
     * ShortList list2 = ShortList.of((short)5, (short)6);
     * short[] array2 = new short[]{(short)5, (short)5, (short)6};
     * ShortList result2 = list2.difference(array2); // result will be [] (empty)
     * // No elements remain because array2 has at least as many occurrences of each value as list2
     * </pre>
     *
     * @param b the array to compare against this list
     * @return a new ShortList containing the elements that are present in this list but not in the specified array,
     *         considering the number of occurrences.
     *         Returns a copy of this list if {@code b} is {@code null} or empty.
     * @see #difference(ShortList)
     * @see #symmetricDifference(short[])
     * @see #intersection(short[])
     * @see N#difference(short[], short[])
     * @see N#difference(int[], int[])
     */
    @Override
    public ShortList difference(final short[] b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(b));
    }

    /**
     * Returns a new ShortList containing elements that are present in either this list or the specified array,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified array.
     *
     * <p>Example:
     * <pre>
     * ShortList list1 = ShortList.of((short)1, (short)1, (short)2, (short)3);
     * short[] array = new short[]{(short)1, (short)2, (short)2, (short)4};
     * ShortList result = list1.symmetricDifference(array);
     * // result will contain: [(short)1, (short)3, (short)2, (short)4]
     * // Elements explanation:
     * // - (short)1 appears twice in list1 and once in array, so one occurrence remains
     * // - (short)3 appears only in list1, so it remains
     * // - (short)2 appears once in list1 and twice in array, so one occurrence remains
     * // - (short)4 appears only in array, so it remains
     * </pre>
     *
     * @param b the array to compare with this list for symmetric difference
     * @return a new ShortList containing elements that are present in either this list or the specified array,
     *         but not in both, considering the number of occurrences
     * @see #symmetricDifference(ShortList)
     * @see #difference(short[])
     * @see #intersection(short[])
     * @see N#symmetricDifference(short[], short[])
     * @see N#symmetricDifference(int[], int[])
     */
    @Override
    public ShortList symmetricDifference(final ShortList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (isEmpty()) {
            return b.copy();
        }

        final Multiset<Short> bOccurrences = b.toMultiset();
        final ShortList c = new ShortList(N.max(9, Math.abs(size() - b.size())));

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
     * Returns a new ShortList containing elements that are present in either this list or the specified array,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified array.
     *
     * <p>Example:
     * <pre>
     * ShortList list1 = ShortList.of((short)1, (short)1, (short)2, (short)3);
     * short[] array = new short[]{(short)1, (short)2, (short)2, (short)4};
     * ShortList result = list1.symmetricDifference(array);
     * // result will contain: [(short)1, (short)3, (short)2, (short)4]
     * // Elements explanation:
     * // - (short)1 appears twice in list1 and once in array, so one occurrence remains
     * // - (short)3 appears only in list1, so it remains
     * // - (short)2 appears once in list1 and twice in array, so one occurrence remains
     * // - (short)4 appears only in array, so it remains
     * </pre>
     *
     * @param b the array to compare with this list for symmetric difference
     * @return a new ShortList containing elements that are present in either this list or the specified array,
     *         but not in both, considering the number of occurrences
     * @see #symmetricDifference(ShortList)
     * @see #difference(short[])
     * @see #intersection(short[])
     * @see N#symmetricDifference(short[], short[])
     * @see N#symmetricDifference(int[], int[])
     */
    @Override
    public ShortList symmetricDifference(final short[] b) {
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
    public int occurrencesOf(final short valueToFind) {
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
     *
     * @param valueToFind
     * @return
     */
    public int indexOf(final short valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     *
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public int indexOf(final short valueToFind, final int fromIndex) {
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
    public int lastIndexOf(final short valueToFind) {
        return lastIndexOf(valueToFind, size);
    }

    /**
     * Last index of.
     * @param valueToFind
     * @param startIndexFromBack the start index to traverse backwards from. Inclusive.
     *
     * @return
     */
    public int lastIndexOf(final short valueToFind, final int startIndexFromBack) {
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

    public OptionalShort min() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(N.min(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalShort min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalShort.empty() : OptionalShort.of(N.min(elementData, fromIndex, toIndex));
    }

    public OptionalShort max() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(N.max(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalShort max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalShort.empty() : OptionalShort.of(N.max(elementData, fromIndex, toIndex));
    }

    public OptionalShort median() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(N.median(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalShort median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalShort.empty() : OptionalShort.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     *
     * @param action
     */
    public void forEach(final ShortConsumer action) {
        forEach(0, size, action);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param action
     * @throws IndexOutOfBoundsException
     */
    public void forEach(final int fromIndex, final int toIndex, final ShortConsumer action) throws IndexOutOfBoundsException {
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
    //    public <E extends Exception> void forEachIndexed(final Throwables.IntShortConsumer<E> action) throws E {
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
    //    public <E extends Exception> void forEachIndexed(final int fromIndex, final int toIndex, final Throwables.IntShortConsumer<E> action)
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

    public OptionalShort first() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(elementData[0]);
    }

    public OptionalShort last() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(elementData[size() - 1]);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    @Override
    public ShortList distinct(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
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
    public int binarySearch(final short valueToFind) {
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
    public int binarySearch(final int fromIndex, final int toIndex, final short valueToFind) throws IndexOutOfBoundsException {
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
    public ShortList copy() {
        return new ShortList(N.copyOfRange(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    @Override
    public ShortList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new ShortList(N.copyOfRange(elementData, fromIndex, toIndex));
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
    public ShortList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), Math.max(fromIndex, toIndex));

        return new ShortList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Returns List of {@code ShortList} with consecutive subsequences of the elements, each of the same size (the final sequence may be smaller).
     *
     * @param fromIndex
     * @param toIndex
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    @Override
    public List<ShortList> split(final int fromIndex, final int toIndex, final int size) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<short[]> list = N.split(elementData, fromIndex, toIndex, size);
        @SuppressWarnings("rawtypes")
        final List<ShortList> result = (List) list;

        for (int i = 0, len = list.size(); i < len; i++) {
            result.set(i, of(list.get(i)));
        }

        return result;
    }

    //    @Override
    //    public List<ShortList> split(int fromIndex, int toIndex, ShortPredicate predicate) {
    //        checkIndex(fromIndex, toIndex);
    //
    //        final List<ShortList> result = new ArrayList<>();
    //        ShortList piece = null;
    //
    //        for (int i = fromIndex; i < toIndex;) {
    //            if (piece == null) {
    //                piece = ShortList.of(N.EMPTY_SHORT_ARRAY);
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
    public ShortList trimToSize() {
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
            N.fill(elementData, 0, size, (short) 0);
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
    public List<Short> boxed() {
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
    public List<Short> boxed(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Short> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    @Override
    public short[] toArray() {
        return N.copyOfRange(elementData, 0, size);
    }

    /**
     * To int list.
     *
     * @return
     */
    public IntList toIntList() {
        final int[] a = new int[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i];//NOSONAR
        }

        return IntList.of(a);
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
    public <C extends Collection<Short>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
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
    public Multiset<Short> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Short>> supplier) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Short> multiset = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            multiset.add(elementData[i]);
        }

        return multiset;
    }

    @Override
    public ShortIterator iterator() {
        if (isEmpty()) {
            return ShortIterator.EMPTY;
        }

        return ShortIterator.of(elementData, 0, size);
    }

    public ShortStream stream() {
        return ShortStream.of(elementData, 0, size());
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public ShortStream stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return ShortStream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in the list.
     *
     * @return The first short value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public short getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in the list.
     *
     * @return The last short value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public short getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list.
     *
     * @param e the element to add
     */
    public void addFirst(final short e) {
        add(0, e);
    }

    /**
     * Inserts the specified element at the end of this list.
     *
     * @param e the element to add
     */
    public void addLast(final short e) {
        add(size, e);
    }

    /**
     * Removes and returns the first element from this list.
     *
     * @return The first short value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public short removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(0);
    }

    /**
     * Removes and returns the last element from this list.
     *
     * @return The last short value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public short removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(size - 1);
    }

    //    /**
    //     * Returns a new ShortList with the elements in reverse order.
    //     *
    //     * @return A new ShortList with all elements of the current list in reverse order.
    //     */
    //    public ShortList reversed() {
    //        final short[] a = N.copyOfRange(elementData, 0, size);
    //
    //        N.reverse(a);
    //
    //        return new ShortList(a);
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
    //    public <R, E extends Exception> R apply(final Throwables.Function<? super ShortList, ? extends R, E> func) throws E {
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
    //    public <R, E extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super ShortList, ? extends R, E> func) throws E {
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
    //    public <E extends Exception> void accept(final Throwables.Consumer<? super ShortList, E> action) throws E {
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
    //    public <E extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super ShortList, E> action) throws E {
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

        if (obj instanceof ShortList other) {
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
            elementData = new short[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            final int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}

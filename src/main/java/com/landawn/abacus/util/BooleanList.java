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
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.stream.Stream;

/**
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 *
 */
public final class BooleanList extends PrimitiveList<Boolean, boolean[], BooleanList> {

    private static final long serialVersionUID = -1194435277403867258L;

    static final Random RAND = new SecureRandom();

    private boolean[] elementData = N.EMPTY_BOOLEAN_ARRAY;

    private int size = 0;

    /**
     * Constructs an empty BooleanList.
     */
    public BooleanList() {
    }

    /**
     * Constructs a BooleanList with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the list
     */
    public BooleanList(final int initialCapacity) {
        elementData = initialCapacity == 0 ? N.EMPTY_BOOLEAN_ARRAY : new boolean[initialCapacity];
    }

    /**
     * Constructs a BooleanList using the specified array as the element array for this list without copying action.
     *
     * @param a the array to be used as the element array for this list
     */
    public BooleanList(final boolean[] a) {
        this(a, a.length);
    }

    /**
     * Constructs a BooleanList using the specified array as the element array for this list without copying action.
     *
     * @param a the array to be used as the element array for this list
     * @param size the number of elements in the list
     * @throws IndexOutOfBoundsException if the specified size is out of bounds
     */
    public BooleanList(final boolean[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     * Creates a BooleanList from the specified array of booleans.
     *
     * @param a the array of booleans to be used as the element array for this list
     * @return a new BooleanList containing the elements of the specified array
     */
    @SafeVarargs
    public static BooleanList of(final boolean... a) {
        return new BooleanList(N.nullToEmpty(a));
    }

    /**
     * Creates a BooleanList from the specified array of booleans and size.
     *
     * @param a the array of booleans to be used as the element array for this list
     * @param size the number of elements in the list
     * @return a new BooleanList containing the elements of the specified array
     * @throws IndexOutOfBoundsException if the specified size is out of bounds
     */
    public static BooleanList of(final boolean[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new BooleanList(N.nullToEmpty(a), size);
    }

    /**
     * Creates a BooleanList that is a copy of the specified array.
     *
     * @param a the array to be copied
     * @return a new BooleanList containing the elements copied from the specified array
     */
    public static BooleanList copyOf(final boolean[] a) {
        return of(N.clone(a));
    }

    /**
     * Creates a BooleanList that is a copy of the specified array within the given range.
     *
     * @param a the array to be copied
     * @param fromIndex the initial index of the range to be copied, inclusive
     * @param toIndex the final index of the range to be copied, exclusive
     * @return a new BooleanList containing the elements copied from the specified array within the given range
     * @throws IndexOutOfBoundsException if the specified range is out of bounds
     */
    public static BooleanList copyOf(final boolean[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * Creates a BooleanList with elements from the specified collection.
     *
     * @param c the collection of Booleans to be used as the element array for this list
     * @return a new BooleanList containing the elements of the specified collection
     */
    public static BooleanList from(final Collection<Boolean> c) {
        if (N.isEmpty(c)) {
            return new BooleanList();
        }

        return from(c, false);
    }

    /**
     * Creates a BooleanList with elements from the specified collection.
     *
     * @param c the collection of Booleans to be used as the element array for this list
     * @param defaultForNull the default boolean value to use if a {@code null} element is encountered in the collection
     * @return a new BooleanList containing the elements of the specified collection
     */
    public static BooleanList from(final Collection<Boolean> c, final boolean defaultForNull) {
        if (N.isEmpty(c)) {
            return new BooleanList();
        }

        final boolean[] a = new boolean[c.size()];
        int idx = 0;

        for (final Boolean e : c) {
            a[idx++] = e == null ? defaultForNull : e;
        }

        return of(a);
    }

    /**
     * Creates a BooleanList with elements from the specified collection within the given range.
     *
     * @param c the collection of Booleans to be used as the element array for this list
     * @param fromIndex the initial index of the range to be copied, inclusive
     * @param toIndex the final index of the range to be copied, exclusive
     * @return a new BooleanList containing the elements of the specified collection within the given range
     * @throws IndexOutOfBoundsException if the specified range is out of bounds
     */
    public static BooleanList from(final Collection<Boolean> c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isEmpty(c)) {
            return new BooleanList();
        }

        return from(c, fromIndex, toIndex, false);
    }

    /**
     * Creates a BooleanList with elements from the specified collection within the given range.
     *
     * @param c the collection of Booleans to be used as the element array for this list
     * @param fromIndex the initial index of the range to be copied, inclusive
     * @param toIndex the final index of the range to be copied, exclusive
     * @param defaultForNull the default boolean value to use if a {@code null} element is encountered in the collection
     * @return a new BooleanList containing the elements of the specified collection within the given range
     * @throws IndexOutOfBoundsException if the specified range is out of bounds
     */
    public static BooleanList from(final Collection<Boolean> c, final int fromIndex, final int toIndex, final boolean defaultForNull) {
        return of(N.toBooleanArray(c, fromIndex, toIndex, defaultForNull));
    }

    /**
     * Creates a BooleanList with the specified element repeated a given number of times.
     *
     * @param element the boolean value to be repeated
     * @param len the number of times to repeat the element
     * @return a new BooleanList containing the repeated elements
     */
    public static BooleanList repeat(final boolean element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Creates a BooleanList with random boolean values.
     *
     * @param len the number of random boolean values to generate
     * @return a new BooleanList containing the random boolean values
     */
    public static BooleanList random(final int len) {
        final boolean[] a = new boolean[len];

        for (int i = 0; i < len; i++) {
            a[i] = RAND.nextBoolean();
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
    public boolean[] array() {
        return elementData;
    }

    /**
     *
     * @param index
     * @return
     */
    public boolean get(final int index) { // NOSONAR
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
    public boolean set(final int index, final boolean e) {
        rangeCheck(index);

        final boolean oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     *
     * @param e
     */
    public void add(final boolean e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     *
     * @param index
     * @param e
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
     * Adds the all.
     *
     * @param c
     * @return
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
     * Adds the all.
     *
     * @param index
     * @param c
     * @return
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
     * Adds the all.
     *
     * @param a
     * @return
     */
    @Override
    public boolean addAll(final boolean[] a) {
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
     * Removes the all occurrences.
     *
     * @param e
     * @return <tt>true</tt> if this list contained the specified element
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
     * Removes the all.
     *
     * @param c
     * @return
     */
    @Override
    public boolean removeAll(final BooleanList c) {
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
    public boolean removeAll(final boolean[] a) {
        if (N.isEmpty(a)) {
            return false;
        }

        return removeAll(of(a));
    }

    /**
     * Removes the if.
     *
     * @param <E>
     * @param p
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean removeIf(final Throwables.BooleanPredicate<E> p) throws E {
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
     *
     * @param c
     * @return
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
     *
     * @param a
     * @return
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
     *
     * @param index
     * @return
     */
    public boolean delete(final int index) {
        rangeCheck(index);

        final boolean oldValue = elementData[index];

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

        final boolean[] tmp = N.deleteAllByIndices(elementData, indices);
        N.copy(tmp, 0, elementData, 0, tmp.length);
        N.fill(elementData, tmp.length, size, false);
        size = tmp.length;
    }

    /**
     *
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

        N.fill(elementData, newSize, size, false);

        this.size = newSize;
    }

    /**
     *
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
     *
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @throws IndexOutOfBoundsException
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
     *
     *
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @throws IndexOutOfBoundsException
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
     *
     * @param oldVal
     * @param newVal
     * @return
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
     *
     * @param <E>
     * @param operator
     * @throws E the e
     */
    public <E extends Exception> void replaceAll(final Throwables.BooleanUnaryOperator<E> operator) throws E {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsBoolean(elementData[i]);
        }
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @param newValue
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean replaceIf(final Throwables.BooleanPredicate<E> predicate, final boolean newValue) throws E {
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
    public void fill(final boolean val) {
        fill(0, size(), val);
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @param val
     * @throws IndexOutOfBoundsException
     */
    public void fill(final int fromIndex, final int toIndex, final boolean val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     *
     * @param valueToFind
     * @return
     */
    public boolean contains(final boolean valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     *
     * @param c
     * @return
     */
    @Override
    public boolean containsAny(final BooleanList c) {
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
    public boolean containsAny(final boolean[] a) {
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
    public boolean containsAll(final BooleanList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        final boolean isThisContainer = size() >= c.size();
        final BooleanList container = isThisContainer ? this : c;
        final boolean[] iterElements = isThisContainer ? c.array() : array();

        if (needToSet(size(), c.size())) {
            final Set<Boolean> set = container.toSet();

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
    public boolean containsAll(final boolean[] a) {
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
    public boolean disjoint(final BooleanList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        final boolean isThisContainer = size() >= c.size();
        final BooleanList container = isThisContainer ? this : c;
        final boolean[] iterElements = isThisContainer ? c.array() : array();

        if (needToSet(size(), c.size())) {
            final Set<Boolean> set = container.toSet();

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
    public boolean disjoint(final boolean[] b) {
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
     * Returns a new list with all the elements occurred in both {@code a} and {@code b}. Occurrences are considered.
     *
     * @param b
     * @return
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
     * Returns a new list with the elements in this list but not in the specified list/array {@code b}. Occurrences are considered.
     *
     * @param b
     * @return
     * @see IntList#difference(IntList)
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
     * Returns a new list with the elements in this list but not in the specified list/array {@code b}. Occurrences are considered.
     *
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    @Override
    public BooleanList difference(final boolean[] b) {
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
     * Returns a new list the elements that are in this list but not in the specified list/array and vice versa. Occurrences are considered
     *
     * @param b
     * @return a new list the elements that are in this list but not in the specified list/array and vice versa. Occurrences are considered
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
     *
     * @param valueToFind
     * @return
     */
    public int occurrencesOf(final boolean valueToFind) {
        return N.occurrencesOf(elementData, valueToFind);
    }

    /**
     *
     * @param valueToFind
     * @return
     */
    public int indexOf(final boolean valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     *
     * @param valueToFind
     * @param fromIndex
     * @return
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
     * Last index of.
     *
     * @param valueToFind
     * @return
     */
    public int lastIndexOf(final boolean valueToFind) {
        return lastIndexOf(valueToFind, size);
    }

    /**
     * Last index of.
     * @param valueToFind
     * @param startIndexFromBack the start index to traverse backwards from. Inclusive.
     *
     * @return
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
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void forEach(final Throwables.BooleanConsumer<E> action) throws E {
        forEach(0, size, action);
    }

    /**
     *
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param action
     * @throws IndexOutOfBoundsException
     * @throws E the e
     */
    public <E extends Exception> void forEach(final int fromIndex, final int toIndex, final Throwables.BooleanConsumer<E> action)
            throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, size);

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
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void forEachIndexed(final Throwables.IntBooleanConsumer<E> action) throws E {
        forEachIndexed(0, size, action);
    }

    /**
     *
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param action
     * @throws IndexOutOfBoundsException
     * @throws E the e
     */
    public <E extends Exception> void forEachIndexed(final int fromIndex, final int toIndex, final Throwables.IntBooleanConsumer<E> action)
            throws IndexOutOfBoundsException, E {
        N.checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, size);

        if (size > 0) {
            if (fromIndex <= toIndex) {
                for (int i = fromIndex; i < toIndex; i++) {
                    action.accept(i, elementData[i]);
                }
            } else {
                for (int i = N.min(size - 1, fromIndex); i > toIndex; i--) {
                    action.accept(i, elementData[i]);
                }
            }
        }
    }

    public OptionalBoolean first() {
        return size() == 0 ? OptionalBoolean.empty() : OptionalBoolean.of(elementData[0]);
    }

    public OptionalBoolean last() {
        return size() == 0 ? OptionalBoolean.empty() : OptionalBoolean.of(elementData[size() - 1]);
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
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
     * Checks for duplicates.
     *
     * @return
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

    @Override
    public boolean isSorted() {
        return N.isSorted(elementData, 0, size);
    }

    /**
     * Sort.
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
    public BooleanList copy() {
        return new BooleanList(N.copyOfRange(elementData, 0, size));
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    @Override
    public BooleanList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new BooleanList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     * @throws IndexOutOfBoundsException
     * @see N#copyOfRange(int[], int, int, int)
     */
    @Override
    public BooleanList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex);

        return new BooleanList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Returns List of {@code BooleanList} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     * @throws IndexOutOfBoundsException
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

    //    @Override
    //    public List<BooleanList> split(int fromIndex, int toIndex, BooleanPredicate predicate) {
    //        checkIndex(fromIndex, toIndex);
    //
    //        final List<BooleanList> result = new ArrayList<>();
    //        BooleanList piece = null;
    //
    //        for (int i = fromIndex; i < toIndex;) {
    //            if (piece == null) {
    //                piece = BooleanList.of(N.EMPTY_BOOLEAN_ARRAY);
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
     *
     *
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    @Override
    public String join(final int fromIndex, final int toIndex, final char delimiter) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return Strings.join(elementData, fromIndex, toIndex, delimiter);
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     * @throws IndexOutOfBoundsException
     */
    @Override
    public String join(final int fromIndex, final int toIndex, final String delimiter) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return Strings.join(elementData, fromIndex, toIndex, delimiter);
    }

    /**
     * Trim to size.
     *
     * @return
     */
    @Override
    public BooleanList trimToSize() {
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
            N.fill(elementData, 0, size, false);
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
    public List<Boolean> boxed() {
        return boxed(0, size);
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
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

    @Override
    public boolean[] toArray() {
        return N.copyOfRange(elementData, 0, size);
    }

    /**
     *
     *
     * @param <C>
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
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
     *
     *
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
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

    @Override
    public BooleanIterator iterator() {
        if (isEmpty()) {
            return BooleanIterator.EMPTY;
        }

        return BooleanIterator.of(elementData, 0, size);
    }

    public Stream<Boolean> stream() {
        return Stream.of(elementData, 0, size());
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Stream<Boolean> stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return Stream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in the list.
     *
     * @return The first boolean value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public boolean getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in the list.
     *
     * @return The last boolean value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public boolean getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list.
     *
     * @param e the element to add
     */
    public void addFirst(final boolean e) {
        add(0, e);
    }

    /**
     * Inserts the specified element at the end of this list.
     *
     * @param e the element to add
     */
    public void addLast(final boolean e) {
        add(size, e);
    }

    /**
     * Removes and returns the first element from this list.
     *
     * @return The first boolean value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public boolean removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(0);
    }

    /**
     * Removes and returns the last element from this list.
     *
     * @return The last boolean value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public boolean removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(size - 1);
    }

    /**
     * Returns a new BooleanList with the elements in reverse order.
     *
     * @return A new BooleanList with all elements of the current list in reverse order.
     */
    public BooleanList reversed() {
        final boolean[] a = N.copyOfRange(elementData, 0, size);

        N.reverse(a);

        return new BooleanList(a);
    }

    /**
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    @Override
    public <R, E extends Exception> R apply(final Throwables.Function<? super BooleanList, ? extends R, E> func) throws E {
        return func.apply(this);
    }

    /**
     * Apply if not empty.
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    @Override
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super BooleanList, ? extends R, E> func) throws E {
        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void accept(final Throwables.Consumer<? super BooleanList, E> action) throws E {
        action.accept(this);
    }

    /**
     * Accept if not empty.
     *
     * @param <E>
     * @param action
     * @return
     * @throws E the e
     */
    @Override
    public <E extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super BooleanList, E> action) throws E {
        return If.is(size > 0).then(this, action);
    }

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

        if (obj instanceof final BooleanList other) {
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
            elementData = new boolean[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            final int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}

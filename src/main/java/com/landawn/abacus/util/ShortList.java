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
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.IntFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.stream.ShortStream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 */
public final class ShortList extends PrimitiveList<Short, short[], ShortList> {

    private static final long serialVersionUID = 25682021483156507L;

    static final Random RAND = new SecureRandom();

    private short[] elementData = N.EMPTY_SHORT_ARRAY;

    private int size = 0;

    /**
     *
     */
    public ShortList() {
    }

    /**
     *
     *
     * @param initialCapacity
     */
    public ShortList(int initialCapacity) {
        elementData = initialCapacity == 0 ? N.EMPTY_SHORT_ARRAY : new short[initialCapacity];
    }

    /**
     * The specified array is used as the element array for this list without copying action.
     *
     * @param a
     */
    public ShortList(short[] a) {
        this(a, a.length);
    }

    /**
     * 
     *
     * @param a 
     * @param size 
     * @throws IndexOutOfBoundsException 
     */
    public ShortList(short[] a, int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        this.elementData = a;
        this.size = size;
    }

    /**
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static ShortList of(final short... a) {
        return new ShortList(N.nullToEmpty(a));
    }

    /**
     * 
     *
     * @param a 
     * @param size 
     * @return 
     * @throws IndexOutOfBoundsException 
     */
    public static ShortList of(final short[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new ShortList(N.nullToEmpty(a), size);
    }

    /**
     *
     * @param a
     * @return
     */
    public static ShortList copyOf(final short[] a) {
        return of(N.clone(a));
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static ShortList copyOf(final short[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     *
     * @param c
     * @return
     */
    public static ShortList from(Collection<Short> c) {
        if (N.isEmpty(c)) {
            return new ShortList();
        }

        return from(c, (short) 0);
    }

    /**
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static ShortList from(Collection<Short> c, short defaultForNull) {
        if (N.isEmpty(c)) {
            return new ShortList();
        }

        final short[] a = new short[c.size()];
        int idx = 0;

        for (Short e : c) {
            a[idx++] = e == null ? defaultForNull : e;
        }

        return of(a);
    }

    /**
     * 
     *
     * @param c 
     * @param fromIndex 
     * @param toIndex 
     * @return 
     * @throws IndexOutOfBoundsException 
     */
    public static ShortList from(final Collection<Short> c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isEmpty(c)) {
            return new ShortList();
        }

        return from(c, fromIndex, toIndex, (short) 0);
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param defaultForNull
     * @return
     */
    public static ShortList from(final Collection<Short> c, final int fromIndex, final int toIndex, short defaultForNull) {
        return of(N.toShortArray(c, fromIndex, toIndex, defaultForNull));
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @return
     */
    public static ShortList range(short startInclusive, final short endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @param by
     * @return
     */
    public static ShortList range(short startInclusive, final short endExclusive, final short by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @return
     */
    public static ShortList rangeClosed(short startInclusive, final short endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @param by
     * @return
     */
    public static ShortList rangeClosed(short startInclusive, final short endInclusive, final short by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     *
     * @param element
     * @param len
     * @return
     */
    public static ShortList repeat(short element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     *
     * @param len
     * @return
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
    public short get(int index) {
        rangeCheck(index);

        return elementData[index];
    }

    /**
     *
     * @param index
     */
    private void rangeCheck(int index) {
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
    public short set(int index, short e) {
        rangeCheck(index);

        short oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     *
     * @param e
     */
    public void add(short e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     *
     * @param index
     * @param e
     */
    public void add(int index, short e) {
        rangeCheckForAdd(index);

        ensureCapacity(size + 1);

        int numMoved = size - index;

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
    public boolean addAll(ShortList c) {
        if (N.isEmpty(c)) {
            return false;
        }

        int numNew = c.size();

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
    public boolean addAll(int index, ShortList c) {
        rangeCheckForAdd(index);

        if (N.isEmpty(c)) {
            return false;
        }

        int numNew = c.size();

        ensureCapacity(size + numNew); // Increments modCount

        int numMoved = size - index;

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
    public boolean addAll(short[] a) {
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
    public boolean addAll(int index, short[] a) {
        rangeCheckForAdd(index);

        if (N.isEmpty(a)) {
            return false;
        }

        int numNew = a.length;

        ensureCapacity(size + numNew); // Increments modCount

        int numMoved = size - index;

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
    private void rangeCheckForAdd(int index) {
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    /**
     *
     * @param e
     * @return <tt>true</tt> if this list contained the specified element
     */
    public boolean remove(short e) {
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
    public boolean removeAllOccurrences(short e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] != e) {
                elementData[w++] = elementData[i];
            }
        }

        int numRemoved = size - w;

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
    private void fastRemove(int index) {
        int numMoved = size - index - 1;

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
    public boolean removeAll(ShortList c) {
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
    public boolean removeAll(short[] a) {
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
    public <E extends Exception> boolean removeIf(Throwables.ShortPredicate<E> p) throws E {
        final ShortList tmp = new ShortList(size());

        for (int i = 0; i < size; i++) {
            if (!p.test(elementData[i])) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == this.size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, this.elementData, 0, tmp.size());
        N.fill(this.elementData, tmp.size(), size, (short) 0);
        size = tmp.size;

        return true;
    }

    /**
     *
     *
     * @return
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
    public boolean retainAll(ShortList c) {
        if (N.isEmpty(c)) {
            boolean result = size() > 0;
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
    public boolean retainAll(short[] a) {
        if (N.isEmpty(a)) {
            boolean result = size() > 0;
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
    private int batchRemove(ShortList c, boolean complement) {
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

        int numRemoved = size - w;

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
    public short delete(int index) {
        rangeCheck(index);

        short oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     *
     * @param indices
     */
    @Override
    @SafeVarargs
    public final void deleteAllByIndices(int... indices) {
        if (N.isEmpty(indices)) {
            return;
        }

        final short[] tmp = N.deleteAllByIndices(elementData, indices);
        N.copy(tmp, 0, elementData, 0, tmp.length);
        N.fill(elementData, tmp.length, size, (short) 0);
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

        N.fill(elementData, newSize, size, (short) 0);

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
    public int replaceAll(short oldVal, short newVal) {
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
    public <E extends Exception> void replaceAll(Throwables.ShortUnaryOperator<E> operator) throws E {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsShort(elementData[i]);
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
    public <E extends Exception> boolean replaceIf(Throwables.ShortPredicate<E> predicate, short newValue) throws E {
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
     * @param e
     * @return
     */
    public boolean contains(short e) {
        return indexOf(e) >= 0;
    }

    /**
     *
     * @param c
     * @return
     */
    @Override
    public boolean containsAny(ShortList c) {
        if (this.isEmpty() || N.isEmpty(c)) {
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
    public boolean containsAny(short[] a) {
        if (this.isEmpty() || N.isEmpty(a)) {
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
    public boolean containsAll(ShortList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        final boolean isThisContainer = size() >= c.size();
        final ShortList container = isThisContainer ? this : c;
        final short[] iterElements = isThisContainer ? c.array() : this.array();

        if (needToSet(size(), c.size())) {
            final Set<Short> set = container.toSet();

            for (int i = 0, iterLen = isThisContainer ? c.size() : this.size(); i < iterLen; i++) {
                if (!set.contains(iterElements[i])) {
                    return false;
                }
            }
        } else {
            for (int i = 0, iterLen = isThisContainer ? c.size() : this.size(); i < iterLen; i++) {
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
    public boolean containsAll(short[] a) {
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

        final boolean isThisContainer = size() >= c.size();
        final ShortList container = isThisContainer ? this : c;
        final short[] iterElements = isThisContainer ? c.array() : this.array();

        if (needToSet(size(), c.size())) {
            final Set<Short> set = container.toSet();

            for (int i = 0, iterLen = isThisContainer ? c.size() : this.size(); i < iterLen; i++) {
                if (set.contains(iterElements[i])) {
                    return false;
                }
            }
        } else {
            for (int i = 0, iterLen = isThisContainer ? c.size() : this.size(); i < iterLen; i++) {
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
    public boolean disjoint(final short[] b) {
        if (isEmpty() || N.isEmpty(b)) {
            return true;
        }

        return disjoint(of(b));
    }

    /**
     *
     * @param b
     * @return
     * @see IntList#intersection(IntList)
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
     *
     * @param a
     * @return
     */
    @Override
    public ShortList intersection(final short[] a) {
        if (N.isEmpty(a)) {
            return new ShortList();
        }

        return intersection(of(a));
    }

    /**
     *
     * @param b
     * @return
     * @see IntList#difference(IntList)
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
     *
     * @param a
     * @return
     */
    @Override
    public ShortList difference(final short[] a) {
        if (N.isEmpty(a)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(a));
    }

    /**
     *
     * @param b
     * @return this.difference(b).addAll(b.difference(this))
     * @see IntList#symmetricDifference(IntList)
     */
    @Override
    public ShortList symmetricDifference(final ShortList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (this.isEmpty()) {
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
     *
     * @param a
     * @return
     */
    @Override
    public ShortList symmetricDifference(final short[] a) {
        if (N.isEmpty(a)) {
            return of(N.copyOfRange(elementData, 0, size()));
        } else if (this.isEmpty()) {
            return of(N.copyOfRange(a, 0, a.length));
        }

        return symmetricDifference(of(a));
    }

    /**
     *
     * @param objectToFind
     * @return
     */
    public int occurrencesOf(final short objectToFind) {
        return N.occurrencesOf(elementData, objectToFind);
    }

    /**
     *
     * @param e
     * @return
     */
    public int indexOf(short e) {
        return indexOf(0, e);
    }

    /**
     *
     * @param fromIndex
     * @param e
     * @return
     */
    public int indexOf(final int fromIndex, short e) {
        if (fromIndex >= size) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.max(fromIndex, 0); i < size; i++) {
            if (elementData[i] == e) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param e
     * @return
     */
    public int lastIndexOf(short e) {
        return lastIndexOf(size, e);
    }

    /**
     * Last index of.
     *
     * @param fromIndex the start index to traverse backwards from. Inclusive.
     * @param e
     * @return
     */
    public int lastIndexOf(final int fromIndex, short e) {
        if (fromIndex < 0 || size == 0) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.min(fromIndex, size - 1); i >= 0; i--) {
            if (elementData[i] == e) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     *
     *
     * @return
     */
    public OptionalShort min() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(N.min(elementData, 0, size));
    }

    /**
     * 
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

    /**
     *
     *
     * @return
     */
    public OptionalShort median() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(N.median(elementData, 0, size));
    }

    /**
     * 
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
     *
     * @return
     */
    public OptionalShort max() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(N.max(elementData, 0, size));
    }

    /**
     * 
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

    /**
     *
     * @param k
     * @return
     */
    public OptionalShort kthLargest(final int k) {
        return kthLargest(0, size(), k);
    }

    /**
     * 
     *
     * @param fromIndex 
     * @param toIndex 
     * @param k 
     * @return 
     * @throws IllegalArgumentException 
     * @throws IndexOutOfBoundsException 
     */
    public OptionalShort kthLargest(final int fromIndex, final int toIndex, final int k) throws IllegalArgumentException, IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);
        N.checkArgPositive(k, "k");

        return toIndex - fromIndex < k ? OptionalShort.empty() : OptionalShort.of(N.kthLargest(elementData, fromIndex, toIndex, k));
    }

    /**
     *
     *
     * @return
     */
    public int sum() {
        return sum(0, size());
    }

    /**
     * 
     *
     * @param fromIndex 
     * @param toIndex 
     * @return 
     * @throws IndexOutOfBoundsException 
     */
    public int sum(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return N.sum(elementData, fromIndex, toIndex);
    }

    /**
     *
     *
     * @return
     */
    public OptionalDouble average() {
        return average(0, size());
    }

    /**
     * 
     *
     * @param fromIndex 
     * @param toIndex 
     * @return 
     * @throws IndexOutOfBoundsException 
     */
    public OptionalDouble average(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalDouble.empty() : OptionalDouble.of(N.average(elementData, fromIndex, toIndex));
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void forEach(Throwables.ShortConsumer<E> action) throws E {
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
    public <E extends Exception> void forEach(final int fromIndex, final int toIndex, Throwables.ShortConsumer<E> action) throws IndexOutOfBoundsException, E {
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
    public <E extends Exception> void forEachIndexed(Throwables.IndexedShortConsumer<E> action) throws E {
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
    public <E extends Exception> void forEachIndexed(final int fromIndex, final int toIndex, Throwables.IndexedShortConsumer<E> action)
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

    /**
     *
     *
     * @return
     */
    public OptionalShort first() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(elementData[0]);
    }

    /**
     *
     *
     * @return
     */
    public OptionalShort last() {
        return size() == 0 ? OptionalShort.empty() : OptionalShort.of(elementData[size() - 1]);
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

    /**
     *
     * @param n
     * @return
     */
    public ShortList top(final int n) {
        return top(0, size(), n);
    }

    /**
     * 
     *
     * @param fromIndex 
     * @param toIndex 
     * @param n 
     * @return 
     * @throws IndexOutOfBoundsException 
     */
    public ShortList top(final int fromIndex, final int toIndex, final int n) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return of(N.top(elementData, fromIndex, toIndex, n));
    }

    /**
     *
     * @param n
     * @param cmp
     * @return
     */
    public ShortList top(final int n, Comparator<? super Short> cmp) {
        return top(0, size(), n, cmp);
    }

    /**
     * 
     *
     * @param fromIndex 
     * @param toIndex 
     * @param n 
     * @param cmp 
     * @return 
     * @throws IndexOutOfBoundsException 
     */
    public ShortList top(final int fromIndex, final int toIndex, final int n, Comparator<? super Short> cmp) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return of(N.top(elementData, fromIndex, toIndex, n, cmp));
    }

    /**
     *
     *
     * @return
     */
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
    public void rotate(int distance) {
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
    public void swap(int i, int j) {
        rangeCheck(i);
        rangeCheck(j);

        set(i, set(j, elementData[i]));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public ShortList copy() {
        return new ShortList(N.copyOfRange(elementData, 0, size));
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
    public ShortList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new ShortList(N.copyOfRange(elementData, fromIndex, toIndex));
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
    public ShortList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex);

        return new ShortList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Returns List of {@code ShortList} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
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
     * 
     *
     * @param fromIndex 
     * @param toIndex 
     * @param delimiter 
     * @return 
     * @throws IndexOutOfBoundsException 
     */
    @Override
    public String join(int fromIndex, int toIndex, char delimiter) throws IndexOutOfBoundsException {
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
    public String join(int fromIndex, int toIndex, String delimiter) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return Strings.join(elementData, fromIndex, toIndex, delimiter);
    }

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
     * @return true, if is empty
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public int size() {
        return size;
    }

    /**
     *
     *
     * @return
     */
    public List<Short> boxed() {
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
    public List<Short> boxed(int fromIndex, int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Short> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    /**
     *
     *
     * @return
     */
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

    /**
     *
     *
     * @return
     */
    public ShortIterator iterator() {
        if (isEmpty()) {
            return ShortIterator.EMPTY;
        }

        return ShortIterator.of(elementData, 0, size);
    }

    /**
     *
     *
     * @return
     */
    public ShortStream stream() {
        return ShortStream.of(elementData, 0, size());
    }

    /**
     * 
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
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    @Override
    public <R, E extends Exception> R apply(Throwables.Function<? super ShortList, ? extends R, E> func) throws E {
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
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(Throwables.Function<? super ShortList, ? extends R, E> func) throws E {
        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void accept(Throwables.Consumer<? super ShortList, E> action) throws E {
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
    public <E extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super ShortList, E> action) throws E {
        return If.is(size > 0).then(this, action);
    }

    /**
     *
     *
     * @return
     */
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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ShortList other) {
            return this.size == other.size && N.equals(this.elementData, 0, other.elementData, 0, this.size);
        }

        return false;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return size == 0 ? "[]" : N.toString(elementData, 0, size);
    }

    private void ensureCapacity(final int minCapacity) {
        if (minCapacity > MAX_ARRAY_SIZE || minCapacity < 0) {
            throw new OutOfMemoryError();
        }

        if (N.isEmpty(elementData)) {
            elementData = new short[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}

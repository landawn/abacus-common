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
import java.util.Random;
import java.util.Set;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.Throwables.Function;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.stream.Stream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class BooleanList extends PrimitiveList<Boolean, boolean[], BooleanList> {

    private static final long serialVersionUID = -1194435277403867258L;

    static final Random RAND = new SecureRandom();

    private boolean[] elementData = N.EMPTY_BOOLEAN_ARRAY;

    private int size = 0;

    public BooleanList() {
        super();
    }

    public BooleanList(int initialCapacity) {
        elementData = initialCapacity == 0 ? N.EMPTY_BOOLEAN_ARRAY : new boolean[initialCapacity];
    }

    /**
     * The specified array is used as the element array for this list without copying action.
     *
     * @param a
     */
    public BooleanList(boolean[] a) {
        this(a, a.length);
    }

    public BooleanList(boolean[] a, int size) {
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
    public static BooleanList of(final boolean... a) {
        return new BooleanList(N.nullToEmpty(a));
    }

    /**
     *
     * @param a
     * @param size
     * @return
     */
    public static BooleanList of(final boolean[] a, final int size) {
        N.checkFromIndexSize(0, size, N.len(a));

        return new BooleanList(N.nullToEmpty(a), size);
    }

    /**
     *
     * @param a
     * @return
     */
    public static BooleanList copyOf(final boolean[] a) {
        return of(N.clone(a));
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static BooleanList copyOf(final boolean[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     *
     * @param c
     * @return
     */
    public static BooleanList from(Collection<Boolean> c) {
        if (N.isNullOrEmpty(c)) {
            return new BooleanList();
        }

        return from(c, false);
    }

    /**
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static BooleanList from(Collection<Boolean> c, boolean defaultForNull) {
        if (N.isNullOrEmpty(c)) {
            return new BooleanList();
        }

        final boolean[] a = new boolean[c.size()];
        int idx = 0;

        for (Boolean e : c) {
            a[idx++] = e == null ? defaultForNull : e;
        }

        return of(a);
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static BooleanList from(final Collection<Boolean> c, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isNullOrEmpty(c)) {
            return new BooleanList();
        }

        return from(c, fromIndex, toIndex, false);
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param defaultForNull
     * @return
     */
    public static BooleanList from(final Collection<Boolean> c, final int fromIndex, final int toIndex, boolean defaultForNull) {
        return of(N.toBooleanArray(c, fromIndex, toIndex, defaultForNull));
    }

    /**
     *
     * @param element
     * @param len
     * @return
     */
    public static BooleanList repeat(boolean element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     *
     * @param len
     * @return
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
    public boolean get(int index) {
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
    public boolean set(int index, boolean e) {
        rangeCheck(index);

        boolean oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     *
     * @param e
     */
    public void add(boolean e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     *
     * @param index
     * @param e
     */
    public void add(int index, boolean e) {
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
    public boolean addAll(BooleanList c) {
        if (N.isNullOrEmpty(c)) {
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
    public boolean addAll(int index, BooleanList c) {
        rangeCheckForAdd(index);

        if (N.isNullOrEmpty(c)) {
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
    public boolean addAll(boolean[] a) {
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
    public boolean addAll(int index, boolean[] a) {
        rangeCheckForAdd(index);

        if (N.isNullOrEmpty(a)) {
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
    public boolean remove(boolean e) {
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
    public boolean removeAllOccurrences(boolean e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] != e) {
                elementData[w++] = elementData[i];
            }
        }

        int numRemoved = size - w;

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
    private void fastRemove(int index) {
        int numMoved = size - index - 1;

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
    public boolean removeAll(BooleanList c) {
        if (N.isNullOrEmpty(c)) {
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
    public boolean removeAll(boolean[] a) {
        if (N.isNullOrEmpty(a)) {
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
    public <E extends Exception> boolean removeIf(Throwables.BooleanPredicate<E> p) throws E {
        final BooleanList tmp = new BooleanList(size());

        for (int i = 0; i < size; i++) {
            if (p.test(elementData[i]) == false) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == this.size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, this.elementData, 0, tmp.size());
        N.fill(this.elementData, tmp.size(), size, false);
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
    public boolean retainAll(BooleanList c) {
        if (N.isNullOrEmpty(c)) {
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
    public boolean retainAll(boolean[] a) {
        if (N.isNullOrEmpty(a)) {
            boolean result = size() > 0;
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
    private int batchRemove(BooleanList c, boolean complement) {
        final boolean[] elementData = this.elementData;

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

        int numRemoved = size - w;

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
    public boolean delete(int index) {
        rangeCheck(index);

        boolean oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     *
     * @param indices
     */
    @Override
    @SafeVarargs
    public final void deleteAll(int... indices) {
        if (N.isNullOrEmpty(indices)) {
            return;
        }

        final boolean[] tmp = N.deleteAll(elementData, indices);
        N.copy(tmp, 0, elementData, 0, tmp.length);
        N.fill(elementData, tmp.length, size, false);
        size = tmp.length;
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     */
    @Override
    public void deleteRange(final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (fromIndex == toIndex) {
            return;
        }

        final int size = size();
        final int newSize = size - (toIndex - fromIndex);

        if (toIndex < size) {
            System.arraycopy(elementData, toIndex, elementData, fromIndex, size - toIndex);
        }

        N.fill(elementData, newSize, size, false);

        this.size = newSize;
    }

    @Override
    public void moveRange(final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        N.moveRange(elementData, fromIndex, toIndex, newPositionStartIndex);
    }

    @Override
    public void replaceRange(final int fromIndex, final int toIndex, final boolean[] replacement) {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (N.isNullOrEmpty(replacement)) {
            deleteRange(fromIndex, toIndex);
            return;
        }

        final int size = this.size;
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
    public int replaceAll(boolean oldVal, boolean newVal) {
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
    public <E extends Exception> void replaceAll(Throwables.BooleanUnaryOperator<E> operator) throws E {
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
    public <E extends Exception> boolean replaceIf(Throwables.BooleanPredicate<E> predicate, boolean newValue) throws E {
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
     * @param fromIndex
     * @param toIndex
     * @param val
     */
    public void fill(final int fromIndex, final int toIndex, final boolean val) {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     *
     * @param e
     * @return
     */
    public boolean contains(boolean e) {
        return indexOf(e) >= 0;
    }

    /**
     *
     * @param c
     * @return
     */
    @Override
    public boolean containsAny(BooleanList c) {
        if (this.isEmpty() || N.isNullOrEmpty(c)) {
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
    public boolean containsAny(boolean[] a) {
        if (this.isEmpty() || N.isNullOrEmpty(a)) {
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
    public boolean containsAll(BooleanList c) {
        if (N.isNullOrEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        final boolean isThisContainer = size() >= c.size();
        final BooleanList container = isThisContainer ? this : c;
        final boolean[] iterElements = isThisContainer ? c.array() : this.array();

        if (needToSet(size(), c.size())) {
            final Set<Boolean> set = container.toSet();

            for (int i = 0, iterLen = isThisContainer ? c.size() : this.size(); i < iterLen; i++) {
                if (set.contains(iterElements[i]) == false) {
                    return false;
                }
            }
        } else {
            for (int i = 0, iterLen = isThisContainer ? c.size() : this.size(); i < iterLen; i++) {
                if (container.contains(iterElements[i]) == false) {
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
    public boolean containsAll(boolean[] a) {
        if (N.isNullOrEmpty(a)) {
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
        if (isEmpty() || N.isNullOrEmpty(c)) {
            return true;
        }

        final boolean isThisContainer = size() >= c.size();
        final BooleanList container = isThisContainer ? this : c;
        final boolean[] iterElements = isThisContainer ? c.array() : this.array();

        if (needToSet(size(), c.size())) {
            final Set<Boolean> set = container.toSet();

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
    public boolean disjoint(final boolean[] b) {
        if (isEmpty() || N.isNullOrEmpty(b)) {
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
    public BooleanList intersection(final BooleanList b) {
        if (N.isNullOrEmpty(b)) {
            return new BooleanList();
        }

        final Multiset<Boolean> bOccurrences = b.toMultiset();

        final BooleanList c = new BooleanList(N.min(9, size(), b.size()));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.getAndRemove(elementData[i]) > 0) {
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
    public BooleanList intersection(final boolean[] a) {
        if (N.isNullOrEmpty(a)) {
            return new BooleanList();
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
    public BooleanList difference(final BooleanList b) {
        if (N.isNullOrEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Boolean> bOccurrences = b.toMultiset();

        final BooleanList c = new BooleanList(N.min(size(), N.max(9, size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.getAndRemove(elementData[i]) < 1) {
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
    public BooleanList difference(final boolean[] a) {
        if (N.isNullOrEmpty(a)) {
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
    public BooleanList symmetricDifference(final BooleanList b) {
        if (N.isNullOrEmpty(b)) {
            return this.copy();
        } else if (this.isEmpty()) {
            return b.copy();
        }

        final Multiset<Boolean> bOccurrences = b.toMultiset();
        final BooleanList c = new BooleanList(N.max(9, Math.abs(size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.getAndRemove(elementData[i]) < 1) {
                c.add(elementData[i]);
            }
        }

        for (int i = 0, len = b.size(); i < len; i++) {
            if (bOccurrences.getAndRemove(b.elementData[i]) > 0) {
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
    public BooleanList symmetricDifference(final boolean[] a) {
        if (N.isNullOrEmpty(a)) {
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
    public int occurrencesOf(final boolean objectToFind) {
        return N.occurrencesOf(elementData, objectToFind);
    }

    /**
     *
     * @param e
     * @return
     */
    public int indexOf(boolean e) {
        return indexOf(0, e);
    }

    /**
     *
     * @param fromIndex
     * @param e
     * @return
     */
    public int indexOf(final int fromIndex, boolean e) {
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
    public int lastIndexOf(boolean e) {
        return lastIndexOf(size, e);
    }

    /**
     * Last index of.
     *
     * @param fromIndex the start index to traverse backwards from. Inclusive.
     * @param e
     * @return
     */
    public int lastIndexOf(final int fromIndex, boolean e) {
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
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void forEach(Throwables.BooleanConsumer<E> action) throws E {
        forEach(0, size, action);
    }

    /**
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void forEach(final int fromIndex, final int toIndex, Throwables.BooleanConsumer<E> action) throws E {
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
    public <E extends Exception> void forEachIndexed(Throwables.IndexedBooleanConsumer<E> action) throws E {
        forEachIndexed(0, size, action);
    }

    /**
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void forEachIndexed(final int fromIndex, final int toIndex, Throwables.IndexedBooleanConsumer<E> action) throws E {
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
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> OptionalBoolean findFirst(Throwables.BooleanPredicate<E> predicate) throws E {
        for (int i = 0; i < size; i++) {
            if (predicate.test(elementData[i])) {
                return OptionalBoolean.of(elementData[i]);
            }
        }

        return OptionalBoolean.empty();
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> OptionalBoolean findLast(Throwables.BooleanPredicate<E> predicate) throws E {
        for (int i = size - 1; i >= 0; i--) {
            if (predicate.test(elementData[i])) {
                return OptionalBoolean.of(elementData[i]);
            }
        }

        return OptionalBoolean.empty();
    }

    /**
     * Find first index.
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> OptionalInt findFirstIndex(Throwables.BooleanPredicate<E> predicate) throws E {
        for (int i = 0; i < size; i++) {
            if (predicate.test(elementData[i])) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Find last index.
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> OptionalInt findLastIndex(Throwables.BooleanPredicate<E> predicate) throws E {
        for (int i = size - 1; i >= 0; i--) {
            if (predicate.test(elementData[i])) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Returns whether all elements of this List match the provided predicate.
     *
     * @param <E>
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean allMatch(Throwables.BooleanPredicate<E> filter) throws E {
        return allMatch(0, size(), filter);
    }

    /**
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean allMatch(final int fromIndex, final int toIndex, Throwables.BooleanPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex);

        if (size > 0) {
            for (int i = fromIndex; i < toIndex; i++) {
                if (filter.test(elementData[i]) == false) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns whether any elements of this List match the provided predicate.
     *
     * @param <E>
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean anyMatch(Throwables.BooleanPredicate<E> filter) throws E {
        return anyMatch(0, size(), filter);
    }

    /**
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean anyMatch(final int fromIndex, final int toIndex, Throwables.BooleanPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex);

        if (size > 0) {
            for (int i = fromIndex; i < toIndex; i++) {
                if (filter.test(elementData[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns whether no elements of this List match the provided predicate.
     *
     * @param <E>
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean noneMatch(Throwables.BooleanPredicate<E> filter) throws E {
        return noneMatch(0, size(), filter);
    }

    /**
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean noneMatch(final int fromIndex, final int toIndex, Throwables.BooleanPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex);

        if (size > 0) {
            for (int i = fromIndex; i < toIndex; i++) {
                if (filter.test(elementData[i])) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     *
     * @param <E>
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> int count(Throwables.BooleanPredicate<E> filter) throws E {
        return count(0, size(), filter);
    }

    /**
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> int count(final int fromIndex, final int toIndex, Throwables.BooleanPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex);

        return N.count(elementData, fromIndex, toIndex, filter);
    }

    /**
     *
     * @param <E>
     * @param filter
     * @return a new List with the elements match the provided predicate.
     * @throws E the e
     */
    public <E extends Exception> BooleanList filter(Throwables.BooleanPredicate<E> filter) throws E {
        return filter(0, size(), filter);
    }

    /**
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> BooleanList filter(final int fromIndex, final int toIndex, Throwables.BooleanPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex);

        return BooleanList.of(N.filter(elementData, fromIndex, toIndex, filter));
    }

    /**
     *
     * @param <E>
     * @param filter
     * @param max
     * @return a new List with the elements match the provided predicate.
     * @throws E the e
     */
    public <E extends Exception> BooleanList filter(Throwables.BooleanPredicate<E> filter, int max) throws E {
        return filter(0, size(), filter, max);
    }

    /**
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public <E extends Exception> BooleanList filter(final int fromIndex, final int toIndex, Throwables.BooleanPredicate<E> filter, final int max) throws E {
        checkFromToIndex(fromIndex, toIndex);

        return BooleanList.of(N.filter(elementData, fromIndex, toIndex, filter, max));
    }

    /**
     *
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <E extends Exception> BooleanList map(final Throwables.BooleanUnaryOperator<E> mapper) throws E {
        return map(0, size, mapper);
    }

    /**
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param mapper
     * @return
     * @throws E the e
     */
    public <E extends Exception> BooleanList map(final int fromIndex, final int toIndex, final Throwables.BooleanUnaryOperator<E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex);

        final BooleanList result = new BooleanList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(mapper.applyAsBoolean(elementData[i]));
        }

        return result;
    }

    /**
     * Map to obj.
     *
     * @param <T>
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <T, E extends Exception> List<T> mapToObj(final Throwables.BooleanFunction<? extends T, E> mapper) throws E {
        return mapToObj(0, size, mapper);
    }

    /**
     * Map to obj.
     *
     * @param <T>
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param mapper
     * @return
     * @throws E the e
     */
    public <T, E extends Exception> List<T> mapToObj(final int fromIndex, final int toIndex, final Throwables.BooleanFunction<? extends T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex);

        final List<T> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(mapper.apply(elementData[i]));
        }

        return result;
    }

    /**
     * This is equivalent to:
     * <pre>
     * <code>
     *    if (isEmpty()) {
     *        return OptionalBoolean.empty();
     *    }
     *
     *    boolean result = elementData[0];
     *
     *    for (int i = 1; i < size; i++) {
     *        result = accumulator.applyAsBoolean(result, elementData[i]);
     *    }
     *
     *    return OptionalBoolean.of(result);
     * </code>
     * </pre>
     *
     * @param <E>
     * @param accumulator
     * @return
     * @throws E the e
     */
    public <E extends Exception> OptionalBoolean reduce(final Throwables.BooleanBinaryOperator<E> accumulator) throws E {
        if (isEmpty()) {
            return OptionalBoolean.empty();
        }

        boolean result = elementData[0];

        for (int i = 1; i < size; i++) {
            result = accumulator.applyAsBoolean(result, elementData[i]);
        }

        return OptionalBoolean.of(result);
    }

    /**
     * This is equivalent to:
     * <pre>
     * <code>
     *     if (isEmpty()) {
     *         return identity;
     *     }
     *
     *     boolean result = identity;
     *
     *     for (int i = 0; i < size; i++) {
     *         result = accumulator.applyAsBoolean(result, elementData[i]);
     *    }
     *
     *     return result;
     * </code>
     * </pre>
     *
     * @param <E>
     * @param identity
     * @param accumulator
     * @return
     * @throws E the e
     */
    public <E extends Exception> boolean reduce(final boolean identity, final Throwables.BooleanBinaryOperator<E> accumulator) throws E {
        if (isEmpty()) {
            return identity;
        }

        boolean result = identity;

        for (int i = 0; i < size; i++) {
            result = accumulator.applyAsBoolean(result, elementData[i]);
        }

        return result;
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    @Override
    public BooleanList distinct(final int fromIndex, final int toIndex) {
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
            count[elementData[i] == false ? 0 : 1]++;
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
     * @param fromIndex
     * @param toIndex
     */
    @Override
    public void reverse(final int fromIndex, final int toIndex) {
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

    @Override
    public BooleanList copy() {
        return new BooleanList(N.copyOfRange(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    @Override
    public BooleanList copy(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return new BooleanList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     * @see N#copyOfRange(int[], int, int, int)
     */
    @Override
    public BooleanList copy(final int fromIndex, final int toIndex, final int step) {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex);

        return new BooleanList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Returns List of {@code BooleanList} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     *
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    @Override
    public List<BooleanList> split(final int fromIndex, final int toIndex, final int chunkSize) {
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
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    @Override
    public String join(int fromIndex, int toIndex, char delimiter) {
        checkFromToIndex(fromIndex, toIndex);

        return StringUtil.join(elementData, fromIndex, toIndex, delimiter);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    @Override
    public String join(int fromIndex, int toIndex, String delimiter) {
        checkFromToIndex(fromIndex, toIndex);

        return StringUtil.join(elementData, fromIndex, toIndex, delimiter);
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
     * @return true, if is empty
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    public List<Boolean> boxed() {
        return boxed(0, size);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public List<Boolean> boxed(int fromIndex, int toIndex) {
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
     * @param <C>
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    @Override
    public <C extends Collection<Boolean>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier) {
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
     */
    @Override
    public Multiset<Boolean> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Boolean>> supplier) {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Boolean> multiset = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            multiset.add(elementData[i]);
        }

        return multiset;
    }

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
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public Stream<Boolean> stream(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return Stream.of(elementData, fromIndex, toIndex);
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
    public <R, E extends Exception> R apply(Throwables.Function<? super BooleanList, R, E> func) throws E {
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
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(Function<? super BooleanList, R, E> func) throws E {
        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void accept(Throwables.Consumer<? super BooleanList, E> action) throws E {
        action.accept(this);
    }

    /**
     * Accept if not empty.
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super BooleanList, E> action) throws E {
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
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof BooleanList) {
            final BooleanList other = (BooleanList) obj;

            return this.size == other.size && N.equals(this.elementData, 0, other.elementData, 0, this.size);
        }

        return false;
    }

    @Override
    public String toString() {
        return size == 0 ? "[]" : N.toString(elementData, 0, size);
    }

    private void ensureCapacity(final int minCapacity) {
        if (minCapacity > MAX_ARRAY_SIZE || minCapacity < 0) {
            throw new OutOfMemoryError();
        }

        if (N.isNullOrEmpty(elementData)) {
            elementData = new boolean[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}

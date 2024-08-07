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
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.stream.FloatStream;

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
public final class FloatList extends PrimitiveList<Float, float[], FloatList> {

    private static final long serialVersionUID = 6459013170687883950L;

    static final Random RAND = new SecureRandom();

    private float[] elementData = N.EMPTY_FLOAT_ARRAY;

    private int size = 0;

    /**
     *
     */
    public FloatList() {
    }

    /**
     *
     *
     * @param initialCapacity
     */
    public FloatList(int initialCapacity) {
        elementData = initialCapacity == 0 ? N.EMPTY_FLOAT_ARRAY : new float[initialCapacity];
    }

    /**
     * The specified array is used as the element array for this list without copying action.
     *
     * @param a
     */
    public FloatList(float[] a) {
        this(a, a.length);
    }

    /**
     *
     *
     * @param a
     * @param size
     * @throws IndexOutOfBoundsException
     */
    public FloatList(float[] a, int size) throws IndexOutOfBoundsException {
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
    public static FloatList of(final float... a) {
        return new FloatList(N.nullToEmpty(a));
    }

    /**
     *
     *
     * @param a
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static FloatList of(final float[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new FloatList(N.nullToEmpty(a), size);
    }

    /**
     *
     * @param a
     * @return
     */
    public static FloatList copyOf(final float[] a) {
        return of(N.clone(a));
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static FloatList copyOf(final float[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     *
     * @param c
     * @return
     */
    public static FloatList from(Collection<Float> c) {
        if (N.isEmpty(c)) {
            return new FloatList();
        }

        return from(c, 0f);
    }

    /**
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static FloatList from(Collection<Float> c, float defaultForNull) {
        if (N.isEmpty(c)) {
            return new FloatList();
        }

        final float[] a = new float[c.size()];
        int idx = 0;

        for (Float e : c) {
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
    public static FloatList from(final Collection<Float> c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isEmpty(c)) {
            return new FloatList();
        }

        return from(c, fromIndex, toIndex, 0);
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param defaultForNull
     * @return
     */
    public static FloatList from(final Collection<Float> c, final int fromIndex, final int toIndex, float defaultForNull) {
        return of(N.toFloatArray(c, fromIndex, toIndex, defaultForNull));
    }

    /**
     *
     * @param element
     * @param len
     * @return
     */
    public static FloatList repeat(float element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     *
     * @param len
     * @return
     */
    public static FloatList random(final int len) {
        final float[] a = new float[len];

        for (int i = 0; i < len; i++) {
            a[i] = RAND.nextFloat();
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
    public float[] array() {
        return elementData;
    }

    /**
     *
     * @param index
     * @return
     */
    public float get(int index) {
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
    public float set(int index, float e) {
        rangeCheck(index);

        float oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     *
     * @param e
     */
    public void add(float e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     *
     * @param index
     * @param e
     */
    public void add(int index, float e) {
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
    public boolean addAll(FloatList c) {
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
    public boolean addAll(int index, FloatList c) {
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
    public boolean addAll(float[] a) {
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
    public boolean addAll(int index, float[] a) {
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
    public boolean remove(float e) {
        for (int i = 0; i < size; i++) {
            if (N.equals(elementData[i], e)) {

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
    public boolean removeAllOccurrences(float e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (!N.equals(elementData[i], e)) {
                elementData[w++] = elementData[i];
            }
        }

        int numRemoved = size - w;

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
    public boolean removeAll(FloatList c) {
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
    public boolean removeAll(float[] a) {
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
    public <E extends Exception> boolean removeIf(Throwables.FloatPredicate<E> p) throws E {
        final FloatList tmp = new FloatList(size());

        for (int i = 0; i < size; i++) {
            if (!p.test(elementData[i])) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == this.size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, this.elementData, 0, tmp.size());
        N.fill(this.elementData, tmp.size(), size, 0f);
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
                if (elementData[i] != elementData[idx]) { // NOSONAR
                    elementData[++idx] = elementData[i];
                }
            }

        } else {
            final Set<Float> set = N.newLinkedHashSet(size);
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
    public boolean retainAll(FloatList c) {
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
    public boolean retainAll(float[] a) {
        if (N.isEmpty(a)) {
            boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(FloatList.of(a));
    }

    /**
     *
     * @param c
     * @param complement
     * @return
     */
    private int batchRemove(FloatList c, boolean complement) {
        final float[] elementData = this.elementData;//NOSONAR

        int w = 0;

        if (c.size() > 3 && size() > 9) {
            final Set<Float> set = c.toSet();

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
    public float delete(int index) {
        rangeCheck(index);

        float oldValue = elementData[index];

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

        final float[] tmp = N.deleteAllByIndices(elementData, indices);
        N.copy(tmp, 0, elementData, 0, tmp.length);
        N.fill(elementData, tmp.length, size, 0f);
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

        N.fill(elementData, newSize, size, 0);

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
    public void replaceRange(final int fromIndex, final int toIndex, final FloatList replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, 0F);
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
    public void replaceRange(final int fromIndex, final int toIndex, final float[] replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, 0F);
        }

        this.size = newSize;
    }

    /**
     *
     * @param oldVal
     * @param newVal
     * @return
     */
    public int replaceAll(float oldVal, float newVal) {
        if (size() == 0) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = size(); i < len; i++) {
            if (Float.compare(elementData[i], oldVal) == 0) {
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
    public <E extends Exception> void replaceAll(Throwables.FloatUnaryOperator<E> operator) throws E {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsFloat(elementData[i]);
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
    public <E extends Exception> boolean replaceIf(Throwables.FloatPredicate<E> predicate, float newValue) throws E {
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
    public void fill(final float val) {
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
    public void fill(final int fromIndex, final int toIndex, final float val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     *
     * @param valueToFind
     * @return
     */
    public boolean contains(final float valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     *
     * @param c
     * @return
     */
    @Override
    public boolean containsAny(FloatList c) {
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
    public boolean containsAny(float[] a) {
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
    public boolean containsAll(FloatList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        final boolean isThisContainer = size() >= c.size();
        final FloatList container = isThisContainer ? this : c;
        final float[] iterElements = isThisContainer ? c.array() : this.array();

        if (needToSet(size(), c.size())) {
            final Set<Float> set = container.toSet();

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
    public boolean containsAll(float[] a) {
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
    public boolean disjoint(final FloatList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        final boolean isThisContainer = size() >= c.size();
        final FloatList container = isThisContainer ? this : c;
        final float[] iterElements = isThisContainer ? c.array() : this.array();

        if (needToSet(size(), c.size())) {
            final Set<Float> set = container.toSet();

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
    public boolean disjoint(final float[] b) {
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
    public FloatList intersection(final FloatList b) {
        if (N.isEmpty(b)) {
            return new FloatList();
        }

        final Multiset<Float> bOccurrences = b.toMultiset();

        final FloatList c = new FloatList(N.min(9, size(), b.size()));

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
    public FloatList intersection(final float[] a) {
        if (N.isEmpty(a)) {
            return new FloatList();
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
    public FloatList difference(FloatList b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Float> bOccurrences = b.toMultiset();

        final FloatList c = new FloatList(N.min(size(), N.max(9, size() - b.size())));

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
    public FloatList difference(final float[] a) {
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
    public FloatList symmetricDifference(FloatList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (this.isEmpty()) {
            return b.copy();
        }

        final Multiset<Float> bOccurrences = b.toMultiset();
        final FloatList c = new FloatList(N.max(9, Math.abs(size() - b.size())));

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
    public FloatList symmetricDifference(final float[] a) {
        if (N.isEmpty(a)) {
            return of(N.copyOfRange(elementData, 0, size()));
        } else if (this.isEmpty()) {
            return of(N.copyOfRange(a, 0, a.length));
        }

        return symmetricDifference(of(a));
    }

    /**
     *
     * @param valueToFind
     * @return
     */
    public int occurrencesOf(final float valueToFind) {
        return N.occurrencesOf(elementData, valueToFind);
    }

    /**
     *
     * @param valueToFind
     * @return
     */
    public int indexOf(final float valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     *
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public int indexOf(final float valueToFind, final int fromIndex) {
        if (fromIndex >= size) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.max(fromIndex, 0); i < size; i++) {
            if (N.equals(elementData[i], valueToFind)) {
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
    public int lastIndexOf(final float valueToFind) {
        return lastIndexOf(valueToFind, size);
    }

    /**
     * Last index of.
     * @param valueToFind
     * @param startIndexFromBack the start index to traverse backwards from. Inclusive.
     *
     * @return
     */
    public int lastIndexOf(final float valueToFind, final int startIndexFromBack) {
        if (startIndexFromBack < 0 || size == 0) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, size - 1); i >= 0; i--) {
            if (N.equals(elementData[i], valueToFind)) {
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
    public OptionalFloat min() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.min(elementData, 0, size));
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalFloat min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalFloat.empty() : OptionalFloat.of(N.min(elementData, fromIndex, toIndex));
    }

    /**
     *
     *
     * @return
     */
    public OptionalFloat median() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.median(elementData, 0, size));
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalFloat median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalFloat.empty() : OptionalFloat.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     *
     *
     * @return
     */
    public OptionalFloat max() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.max(elementData, 0, size));
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalFloat max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalFloat.empty() : OptionalFloat.of(N.max(elementData, fromIndex, toIndex));
    }

    /**
     *
     * @param k
     * @return
     */
    public OptionalFloat kthLargest(final int k) {
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
    public OptionalFloat kthLargest(final int fromIndex, final int toIndex, final int k) throws IllegalArgumentException, IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);
        N.checkArgPositive(k, "k");

        return toIndex - fromIndex < k ? OptionalFloat.empty() : OptionalFloat.of(N.kthLargest(elementData, fromIndex, toIndex, k));
    }

    /**
     *
     *
     * @return
     */
    public float sum() {
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
    public float sum(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
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
    public <E extends Exception> void forEach(Throwables.FloatConsumer<E> action) throws E {
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
    public <E extends Exception> void forEach(final int fromIndex, final int toIndex, Throwables.FloatConsumer<E> action) throws IndexOutOfBoundsException, E {
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
    public <E extends Exception> void forEachIndexed(Throwables.IntFloatConsumer<E> action) throws E {
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
    public <E extends Exception> void forEachIndexed(final int fromIndex, final int toIndex, Throwables.IntFloatConsumer<E> action)
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
    public OptionalFloat first() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(elementData[0]);
    }

    /**
     *
     *
     * @return
     */
    public OptionalFloat last() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(elementData[size() - 1]);
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
    public FloatList distinct(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
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
    public FloatList top(final int n) {
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
    public FloatList top(final int fromIndex, final int toIndex, final int n) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return of(N.top(elementData, fromIndex, toIndex, n));
    }

    /**
     *
     * @param n
     * @param cmp
     * @return
     */
    public FloatList top(final int n, Comparator<? super Float> cmp) {
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
    public FloatList top(final int fromIndex, final int toIndex, final int n, Comparator<? super Float> cmp) throws IndexOutOfBoundsException {
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
    public int binarySearch(final float valueToFind) {
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
    public int binarySearch(final int fromIndex, final int toIndex, final float valueToFind) throws IndexOutOfBoundsException {
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
    public FloatList copy() {
        return new FloatList(N.copyOfRange(elementData, 0, size));
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
    public FloatList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new FloatList(N.copyOfRange(elementData, fromIndex, toIndex));
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
    public FloatList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex);

        return new FloatList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Returns List of {@code FloatList} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     * @throws IndexOutOfBoundsException
     */
    @Override
    public List<FloatList> split(final int fromIndex, final int toIndex, final int chunkSize) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<float[]> list = N.split(elementData, fromIndex, toIndex, chunkSize);
        @SuppressWarnings("rawtypes")
        final List<FloatList> result = (List) list;

        for (int i = 0, len = list.size(); i < len; i++) {
            result.set(i, of(list.get(i)));
        }

        return result;
    }

    //    @Override
    //    public List<FloatList> split(int fromIndex, int toIndex, FloatPredicate predicate) {
    //        checkIndex(fromIndex, toIndex);
    //
    //        final List<FloatList> result = new ArrayList<>();
    //        FloatList piece = null;
    //
    //        for (int i = fromIndex; i < toIndex;) {
    //            if (piece == null) {
    //                piece = FloatList.of(N.EMPTY_FLOAT_ARRAY);
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
    public FloatList trimToSize() {
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
    @Override
    public List<Float> boxed() {
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
    public List<Float> boxed(int fromIndex, int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Float> res = new ArrayList<>(toIndex - fromIndex);

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
    public float[] toArray() {
        return N.copyOfRange(elementData, 0, size);
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
     *
     * @param <C>
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
     */
    @Override
    public <C extends Collection<Float>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
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
    public Multiset<Float> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Float>> supplier) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Float> multiset = supplier.apply(toIndex - fromIndex);

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
    @Override
    public FloatIterator iterator() {
        if (isEmpty()) {
            return FloatIterator.EMPTY;
        }

        return FloatIterator.of(elementData, 0, size);
    }

    /**
     *
     *
     * @return
     */
    public FloatStream stream() {
        return FloatStream.of(elementData, 0, size());
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public FloatStream stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return FloatStream.of(elementData, fromIndex, toIndex);
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
    public <R, E extends Exception> R apply(Throwables.Function<? super FloatList, ? extends R, E> func) throws E {
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
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(Throwables.Function<? super FloatList, ? extends R, E> func) throws E {
        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void accept(Throwables.Consumer<? super FloatList, E> action) throws E {
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
    public <E extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super FloatList, E> action) throws E {
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

        if (obj instanceof FloatList other) {
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
            elementData = new float[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}

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
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.Try.Function;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.Collector;
import com.landawn.abacus.util.stream.FloatStream;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class FloatList extends PrimitiveList<Float, float[], FloatList> {
    private static final long serialVersionUID = 6459013170687883950L;

    static final Random RAND = new SecureRandom();

    private float[] elementData = N.EMPTY_FLOAT_ARRAY;
    private int size = 0;

    public FloatList() {
        super();
    }

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

    public FloatList(float[] a, int size) {
        N.checkFromIndexSize(0, size, a.length);

        this.elementData = a;
        this.size = size;
    }

    @SafeVarargs
    public static FloatList of(final float... a) {
        return new FloatList(N.nullToEmpty(a));
    }

    public static FloatList of(final float[] a, final int size) {
        N.checkFromIndexSize(0, size, N.len(a));

        return new FloatList(N.nullToEmpty(a), size);
    }

    public static FloatList copyOf(final float[] a) {
        return of(N.clone(a));
    }

    public static FloatList copyOf(final float[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    public static FloatList from(Collection<Float> c) {
        if (N.isNullOrEmpty(c)) {
            return new FloatList();
        }

        return from(c, 0f);
    }

    public static FloatList from(Collection<Float> c, float defaultForNull) {
        if (N.isNullOrEmpty(c)) {
            return new FloatList();
        }

        final float[] a = new float[c.size()];
        int idx = 0;

        for (Float e : c) {
            a[idx++] = e == null ? defaultForNull : e;
        }

        return of(a);
    }

    public static FloatList from(final Collection<Float> c, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isNullOrEmpty(c)) {
            return new FloatList();
        }

        return from(c, fromIndex, toIndex, 0);
    }

    public static FloatList from(final Collection<Float> c, final int fromIndex, final int toIndex, float defaultForNull) {
        return of(N.toFloatArray(c, fromIndex, toIndex, defaultForNull));
    }

    public static FloatList repeat(float element, final int len) {
        return of(Array.repeat(element, len));
    }

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
    @Override
    public float[] array() {
        return elementData;
    }

    public float get(int index) {
        rangeCheck(index);

        return elementData[index];
    }

    private void rangeCheck(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    /**
     * 
     * @param index
     * @param e
     * @return the old value in the specified position.
     */
    public float set(int index, float e) {
        rangeCheck(index);

        float oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    public void add(float e) {
        ensureCapacityInternal(size + 1);

        elementData[size++] = e;
    }

    public void add(int index, float e) {
        rangeCheckForAdd(index);

        ensureCapacityInternal(size + 1);

        int numMoved = size - index;

        if (numMoved > 0) {
            N.copy(elementData, index, elementData, index + 1, numMoved);
        }

        elementData[index] = e;

        size++;
    }

    public boolean addAll(FloatList c) {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        int numNew = c.size();

        ensureCapacityInternal(size + numNew);

        N.copy(c.array(), 0, elementData, size, numNew);

        size += numNew;

        return true;
    }

    public boolean addAll(int index, FloatList c) {
        rangeCheckForAdd(index);

        if (N.isNullOrEmpty(c)) {
            return false;
        }

        int numNew = c.size();

        ensureCapacityInternal(size + numNew); // Increments modCount

        int numMoved = size - index;

        if (numMoved > 0) {
            N.copy(elementData, index, elementData, index + numNew, numMoved);
        }

        N.copy(c.array(), 0, elementData, index, numNew);

        size += numNew;

        return true;
    }

    @Override
    public boolean addAll(float[] a) {
        return addAll(size(), a);
    }

    @Override
    public boolean addAll(int index, float[] a) {
        rangeCheckForAdd(index);

        if (N.isNullOrEmpty(a)) {
            return false;
        }

        int numNew = a.length;

        ensureCapacityInternal(size + numNew); // Increments modCount

        int numMoved = size - index;

        if (numMoved > 0) {
            N.copy(elementData, index, elementData, index + numNew, numMoved);
        }

        N.copy(a, 0, elementData, index, numNew);

        size += numNew;

        return true;
    }

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

    private void fastRemove(int index) {
        int numMoved = size - index - 1;

        if (numMoved > 0) {
            N.copy(elementData, index + 1, elementData, index, numMoved);
        }

        elementData[--size] = 0; // clear to let GC do its work
    }

    public boolean removeAll(FloatList c) {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        return batchRemove(c, false) > 0;
    }

    @Override
    public boolean removeAll(float[] a) {
        if (N.isNullOrEmpty(a)) {
            return false;
        }

        return removeAll(of(a));
    }

    public <E extends Exception> boolean removeIf(Try.FloatPredicate<E> p) throws E {
        final FloatList tmp = new FloatList(size());

        for (int i = 0; i < size; i++) {
            if (p.test(elementData[i]) == false) {
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

    public boolean retainAll(FloatList c) {
        if (N.isNullOrEmpty(c)) {
            boolean result = size() > 0;
            clear();
            return result;
        }

        return batchRemove(c, true) > 0;
    }

    public boolean retainAll(float[] a) {
        if (N.isNullOrEmpty(a)) {
            boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(FloatList.of(a));
    }

    private int batchRemove(FloatList c, boolean complement) {
        final float[] elementData = this.elementData;

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
     * @return the deleted element
     */
    public float delete(int index) {
        rangeCheck(index);

        float oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    @Override
    @SafeVarargs
    public final void deleteAll(int... indices) {
        final float[] tmp = N.deleteAll(elementData, indices);
        N.copy(tmp, 0, elementData, 0, tmp.length);
        N.fill(elementData, tmp.length, size, 0f);
        size = tmp.length;

    }

    @Override
    public void deleteRange(final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, size());

        if (fromIndex == toIndex) {
            return;
        }

        final int newSize = size() - (toIndex - fromIndex);

        if (toIndex < size()) {
            System.arraycopy(elementData, toIndex, elementData, fromIndex, size - toIndex);
        }

        N.fill(elementData, newSize, size(), 0);

        size = newSize;
    }

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

    public <E extends Exception> void replaceAll(Try.FloatUnaryOperator<E> operator) throws E {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsFloat(elementData[i]);
        }
    }

    public <E extends Exception> boolean replaceIf(Try.FloatPredicate<E> predicate, float newValue) throws E {
        boolean result = false;

        for (int i = 0, len = size(); i < len; i++) {
            if (predicate.test(elementData[i])) {
                elementData[i] = newValue;

                result = true;
            }
        }

        return result;
    }

    public void fill(final float val) {
        fill(0, size(), val);
    }

    public void fill(final int fromIndex, final int toIndex, final float val) {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    public boolean contains(float e) {
        return indexOf(e) >= 0;
    }

    public boolean containsAll(FloatList c) {
        if (N.isNullOrEmpty(c)) {
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

    @Override
    public boolean containsAll(float[] a) {
        if (N.isNullOrEmpty(a)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        return containsAll(of(a));
    }

    public boolean containsAny(FloatList c) {
        if (this.isEmpty() || N.isNullOrEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    @Override
    public boolean containsAny(float[] a) {
        if (this.isEmpty() || N.isNullOrEmpty(a)) {
            return false;
        }

        return !disjoint(a);
    }

    public boolean disjoint(final FloatList c) {
        if (isEmpty() || N.isNullOrEmpty(c)) {
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

    @Override
    public boolean disjoint(final float[] b) {
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
    public FloatList intersection(final FloatList b) {
        if (N.isNullOrEmpty(b)) {
            return new FloatList();
        }

        final Multiset<Float> bOccurrences = b.toMultiset();

        final FloatList c = new FloatList(N.min(9, size(), b.size()));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.getAndRemove(elementData[i]) > 0) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    public FloatList intersection(final float[] a) {
        if (N.isNullOrEmpty(a)) {
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
    public FloatList difference(FloatList b) {
        if (N.isNullOrEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Float> bOccurrences = b.toMultiset();

        final FloatList c = new FloatList(N.min(size(), N.max(9, size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.getAndRemove(elementData[i]) < 1) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    public FloatList difference(final float[] a) {
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
    public FloatList symmetricDifference(FloatList b) {
        if (N.isNullOrEmpty(b)) {
            return this.copy();
        } else if (this.isEmpty()) {
            return b.copy();
        }

        final Multiset<Float> bOccurrences = b.toMultiset();
        final FloatList c = new FloatList(N.max(9, Math.abs(size() - b.size())));

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

    public FloatList symmetricDifference(final float[] a) {
        if (N.isNullOrEmpty(a)) {
            return of(N.copyOfRange(elementData, 0, size()));
        } else if (this.isEmpty()) {
            return of(N.copyOfRange(a, 0, a.length));
        }

        return symmetricDifference(of(a));
    }

    public int occurrencesOf(final float objectToFind) {
        return N.occurrencesOf(elementData, objectToFind);
    }

    public int indexOf(float e) {
        return indexOf(0, e);
    }

    public int indexOf(final int fromIndex, float e) {
        checkFromToIndex(fromIndex, size);

        for (int i = fromIndex; i < size; i++) {
            if (N.equals(elementData[i], e)) {
                return i;
            }
        }

        return -1;
    }

    public int lastIndexOf(float e) {
        return lastIndexOf(size, e);
    }

    /**
     * 
     * @param fromIndex the start index to traverse backwards from. Inclusive.
     * @param e
     * @return
     */
    public int lastIndexOf(final int fromIndex, float e) {
        checkFromToIndex(0, fromIndex);

        for (int i = fromIndex == size ? size - 1 : fromIndex; i >= 0; i--) {
            if (N.equals(elementData[i], e)) {
                return i;
            }
        }

        return -1;
    }

    public OptionalFloat min() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.min(elementData, 0, size));
    }

    public OptionalFloat min(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalFloat.empty() : OptionalFloat.of(N.min(elementData, fromIndex, toIndex));
    }

    public OptionalFloat median() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.median(elementData, 0, size));
    }

    public OptionalFloat median(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalFloat.empty() : OptionalFloat.of(N.median(elementData, fromIndex, toIndex));
    }

    public OptionalFloat max() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(N.max(elementData, 0, size));
    }

    public OptionalFloat max(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalFloat.empty() : OptionalFloat.of(N.max(elementData, fromIndex, toIndex));
    }

    public OptionalFloat kthLargest(final int k) {
        return kthLargest(0, size(), k);
    }

    public OptionalFloat kthLargest(final int fromIndex, final int toIndex, final int k) {
        checkFromToIndex(fromIndex, toIndex);
        N.checkArgPositive(k, "k");

        return toIndex - fromIndex < k ? OptionalFloat.empty() : OptionalFloat.of(N.kthLargest(elementData, fromIndex, toIndex, k));
    }

    public float sum() {
        return sum(0, size());
    }

    public float sum(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return N.sum(elementData, fromIndex, toIndex);
    }

    public OptionalDouble average() {
        return average(0, size());
    }

    public OptionalDouble average(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalDouble.empty() : OptionalDouble.of(N.average(elementData, fromIndex, toIndex));
    }

    public <E extends Exception> void forEach(Try.FloatConsumer<E> action) throws E {
        forEach(0, size, action);
    }

    public <E extends Exception> void forEach(final int fromIndex, final int toIndex, Try.FloatConsumer<E> action) throws E {
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

    public OptionalFloat first() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(elementData[0]);
    }

    public OptionalFloat last() {
        return size() == 0 ? OptionalFloat.empty() : OptionalFloat.of(elementData[size() - 1]);
    }

    public <E extends Exception> OptionalFloat findFirst(Try.FloatPredicate<E> predicate) throws E {
        for (int i = 0; i < size; i++) {
            if (predicate.test(elementData[i])) {
                return OptionalFloat.of(elementData[i]);
            }
        }

        return OptionalFloat.empty();
    }

    public <E extends Exception> OptionalFloat findLast(Try.FloatPredicate<E> predicate) throws E {
        for (int i = size - 1; i >= 0; i--) {
            if (predicate.test(elementData[i])) {
                return OptionalFloat.of(elementData[i]);
            }
        }

        return OptionalFloat.empty();
    }

    public <E extends Exception> OptionalInt findFirstIndex(Try.FloatPredicate<E> predicate) throws E {
        for (int i = 0; i < size; i++) {
            if (predicate.test(elementData[i])) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    public <E extends Exception> OptionalInt findLastIndex(Try.FloatPredicate<E> predicate) throws E {
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
     * @param filter
     * @return
     */
    public <E extends Exception> boolean allMatch(Try.FloatPredicate<E> filter) throws E {
        return allMatch(0, size(), filter);
    }

    public <E extends Exception> boolean allMatch(final int fromIndex, final int toIndex, Try.FloatPredicate<E> filter) throws E {
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
     * @param filter
     * @return
     */
    public <E extends Exception> boolean anyMatch(Try.FloatPredicate<E> filter) throws E {
        return anyMatch(0, size(), filter);
    }

    public <E extends Exception> boolean anyMatch(final int fromIndex, final int toIndex, Try.FloatPredicate<E> filter) throws E {
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
     * @param filter
     * @return
     */
    public <E extends Exception> boolean noneMatch(Try.FloatPredicate<E> filter) throws E {
        return noneMatch(0, size(), filter);
    }

    public <E extends Exception> boolean noneMatch(final int fromIndex, final int toIndex, Try.FloatPredicate<E> filter) throws E {
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
     * @param filter
     * @return
     */
    public <E extends Exception> int count(Try.FloatPredicate<E> filter) throws E {
        return count(0, size(), filter);
    }

    public <E extends Exception> int count(final int fromIndex, final int toIndex, Try.FloatPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex);

        return N.count(elementData, fromIndex, toIndex, filter);
    }

    /**
     * 
     * @param filter
     * @return a new List with the elements match the provided predicate.
     */
    public <E extends Exception> FloatList filter(Try.FloatPredicate<E> filter) throws E {
        return filter(0, size(), filter);
    }

    public <E extends Exception> FloatList filter(final int fromIndex, final int toIndex, Try.FloatPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex);

        return N.filter(elementData, fromIndex, toIndex, filter);
    }

    /**
     * 
     * @param filter
     * @return a new List with the elements match the provided predicate.
     */
    public <E extends Exception> FloatList filter(Try.FloatPredicate<E> filter, int max) throws E {
        return filter(0, size(), filter, max);
    }

    public <E extends Exception> FloatList filter(final int fromIndex, final int toIndex, Try.FloatPredicate<E> filter, final int max) throws E {
        checkFromToIndex(fromIndex, toIndex);

        return N.filter(elementData, fromIndex, toIndex, filter, max);
    }

    public <E extends Exception> FloatList map(final Try.FloatUnaryOperator<E> mapper) throws E {
        return map(0, size, mapper);
    }

    public <E extends Exception> FloatList map(final int fromIndex, final int toIndex, final Try.FloatUnaryOperator<E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex);

        final FloatList result = new FloatList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(mapper.applyAsFloat(elementData[i]));
        }

        return result;
    }

    public <T, E extends Exception> List<T> mapToObj(final Try.FloatFunction<? extends T, E> mapper) throws E {
        return mapToObj(0, size, mapper);
    }

    public <T, E extends Exception> List<T> mapToObj(final int fromIndex, final int toIndex, final Try.FloatFunction<? extends T, E> mapper) throws E {
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
     *        return OptionalFloat.empty();
     *    }
     *
     *    float result = elementData[0];
     *
     *    for (int i = 1; i < size; i++) {
     *        result = accumulator.applyAsFloat(result, elementData[i]);
     *    }
     *
     *    return OptionalFloat.of(result);
     * </code>
     * </pre>
     * 
     * @param accumulator
     * @return
     */
    public <E extends Exception> OptionalFloat reduce(final Try.FloatBinaryOperator<E> accumulator) throws E {
        if (isEmpty()) {
            return OptionalFloat.empty();
        }

        float result = elementData[0];

        for (int i = 1; i < size; i++) {
            result = accumulator.applyAsFloat(result, elementData[i]);
        }

        return OptionalFloat.of(result);
    }

    /**
     * This is equivalent to:
     * <pre>
     * <code>
     *     if (isEmpty()) {
     *         return identity;
     *     }
     * 
     *     float result = identity;
     * 
     *     for (int i = 0; i < size; i++) {
     *         result = accumulator.applyAsFloat(result, elementData[i]);
     *    }
     * 
     *     return result;
     * </code>
     * </pre>
     * 
     * @param identity
     * @param accumulator
     * @return
     */
    public <E extends Exception> float reduce(final float identity, final Try.FloatBinaryOperator<E> accumulator) throws E {
        if (isEmpty()) {
            return identity;
        }

        float result = identity;

        for (int i = 0; i < size; i++) {
            result = accumulator.applyAsFloat(result, elementData[i]);
        }

        return result;
    }

    @Override
    public boolean hasDuplicates() {
        return N.hasDuplicates(elementData, 0, size, false);
    }

    @Override
    public FloatList distinct(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            return of(N.distinct(elementData, fromIndex, toIndex));
        } else {
            return of(N.copyOfRange(elementData, fromIndex, toIndex));
        }
    }

    public FloatList top(final int n) {
        return top(0, size(), n);
    }

    public FloatList top(final int fromIndex, final int toIndex, final int n) {
        checkFromToIndex(fromIndex, toIndex);

        return of(N.top(elementData, fromIndex, toIndex, n));
    }

    public FloatList top(final int n, Comparator<? super Float> cmp) {
        return top(0, size(), n, cmp);
    }

    public FloatList top(final int fromIndex, final int toIndex, final int n, Comparator<? super Float> cmp) {
        checkFromToIndex(fromIndex, toIndex);

        return of(N.top(elementData, fromIndex, toIndex, n, cmp));
    }

    @Override
    public void sort() {
        if (size > 1) {
            N.sort(elementData, 0, size);
        }
    }

    public void parallelSort() {
        if (size > 1) {
            N.parallelSort(elementData, 0, size);
        }
    }

    public void reverseSort() {
        if (size > 1) {
            sort();
            reverse();
        }
    }

    /**
     * This List should be sorted first.
     * 
     * @param key
     * @return
     */
    public int binarySearch(final float key) {
        return N.binarySearch(elementData, key);
    }

    /**
     * This List should be sorted first.
     *
     * @param fromIndex
     * @param toIndex
     * @param key
     * @return
     */
    public int binarySearch(final int fromIndex, final int toIndex, final float key) {
        checkFromToIndex(fromIndex, toIndex);

        return N.binarySearch(elementData, fromIndex, toIndex, key);
    }

    @Override
    public void reverse() {
        if (size > 1) {
            N.reverse(elementData, 0, size);
        }
    }

    @Override
    public void reverse(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            N.reverse(elementData, fromIndex, toIndex);
        }
    }

    @Override
    public void rotate(int distance) {
        if (size > 1) {
            N.rotate(elementData, distance);
        }
    }

    @Override
    public void shuffle() {
        if (size() > 1) {
            N.shuffle(elementData);
        }
    }

    @Override
    public void shuffle(final Random rnd) {
        if (size() > 1) {
            N.shuffle(elementData, rnd);
        }
    }

    @Override
    public void swap(int i, int j) {
        rangeCheck(i);
        rangeCheck(j);

        set(i, set(j, elementData[i]));
    }

    @Override
    public FloatList copy() {
        return new FloatList(N.copyOfRange(elementData, 0, size));
    }

    @Override
    public FloatList copy(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return new FloatList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     * @param from
     * @param to
     * @param step
     * 
     * @see N#copyOfRange(int[], int, int, int)
     */
    @Override
    public FloatList copy(final int from, final int to, final int step) {
        checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from);

        return new FloatList(N.copyOfRange(elementData, from, to, step));
    }

    /**
     * Returns List of {@code FloatList} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *  
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     */
    @Override
    public List<FloatList> split(final int fromIndex, final int toIndex, final int chunkSize) {
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

    @Override
    public String join(int fromIndex, int toIndex, char delimiter) {
        checkFromToIndex(fromIndex, toIndex);

        return StringUtil.join(elementData, fromIndex, toIndex, delimiter);
    }

    @Override
    public String join(int fromIndex, int toIndex, String delimiter) {
        checkFromToIndex(fromIndex, toIndex);

        return StringUtil.join(elementData, fromIndex, toIndex, delimiter);
    }

    @Override
    public FloatList trimToSize() {
        if (elementData.length > size) {
            elementData = N.copyOfRange(elementData, 0, size);
        }

        return this;
    }

    @Override
    public void clear() {
        if (size > 0) {
            N.fill(elementData, 0, size, 0);
        }

        size = 0;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    public List<Float> boxed() {
        return boxed(0, size);
    }

    public List<Float> boxed(int fromIndex, int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        final List<Float> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    @Override
    public float[] toArray() {
        return N.copyOfRange(elementData, 0, size);
    }

    public DoubleList toDoubleList() {
        final double[] a = new double[size];

        for (int i = 0; i < size; i++) {
            a[i] = elementData[i];
        }

        return DoubleList.of(a);
    }

    @Override
    public <C extends Collection<Float>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier) {
        checkFromToIndex(fromIndex, toIndex);

        final C c = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            c.add(elementData[i]);
        }

        return c;
    }

    @Override
    public Multiset<Float> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Float>> supplier) {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Float> multiset = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            multiset.add(elementData[i]);
        }

        return multiset;
    }

    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Try.FloatFunction<? extends K, E> keyMapper,
            Try.FloatFunction<? extends V, E2> valueMapper) throws E, E2 {
        return toMap(keyMapper, valueMapper, Factory.<K, V> ofMap());
    }

    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Try.FloatFunction<? extends K, E> keyMapper,
            Try.FloatFunction<? extends V, E2> valueMapper, IntFunction<? extends M> mapFactory) throws E, E2 {
        final Try.BinaryOperator<V, RuntimeException> mergeFunction = Fn.throwingMerger();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    public <K, V, E extends Exception, E2 extends Exception, E3 extends Exception> Map<K, V> toMap(Try.FloatFunction<? extends K, E> keyMapper,
            Try.FloatFunction<? extends V, E2> valueMapper, Try.BinaryOperator<V, E3> mergeFunction) throws E, E2, E3 {
        return toMap(keyMapper, valueMapper, mergeFunction, Factory.<K, V> ofMap());
    }

    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception, E3 extends Exception> M toMap(Try.FloatFunction<? extends K, E> keyMapper,
            Try.FloatFunction<? extends V, E2> valueMapper, Try.BinaryOperator<V, E3> mergeFunction, IntFunction<? extends M> mapFactory) throws E, E2, E3 {
        final M result = mapFactory.apply(size);

        for (int i = 0; i < size; i++) {
            Maps.merge(result, keyMapper.apply(elementData[i]), valueMapper.apply(elementData[i]), mergeFunction);
        }

        return result;
    }

    public <K, A, D, E extends Exception> Map<K, D> toMap(Try.FloatFunction<? extends K, E> keyMapper, Collector<Float, A, D> downstream) throws E {
        return toMap(keyMapper, downstream, Factory.<K, D> ofMap());
    }

    public <K, A, D, M extends Map<K, D>, E extends Exception> M toMap(final Try.FloatFunction<? extends K, E> keyMapper,
            final Collector<Float, A, D> downstream, final IntFunction<? extends M> mapFactory) throws E {
        final M result = mapFactory.apply(size);
        final Supplier<A> downstreamSupplier = downstream.supplier();
        final BiConsumer<A, Float> downstreamAccumulator = downstream.accumulator();
        final Map<K, A> intermediate = (Map<K, A>) result;
        K key = null;
        A v = null;

        for (int i = 0; i < size; i++) {
            key = N.checkArgNotNull(keyMapper.apply(elementData[i]), "element cannot be mapped to a null key");

            if ((v = intermediate.get(key)) == null) {
                if ((v = downstreamSupplier.get()) != null) {
                    intermediate.put(key, v);
                }
            }

            downstreamAccumulator.accept(v, elementData[i]);
        }

        final BiFunction<? super K, ? super A, ? extends A> function = new BiFunction<K, A, A>() {
            @Override
            public A apply(K k, A v) {
                return (A) downstream.finisher().apply(v);
            }
        };

        Maps.replaceAll(intermediate, function);

        return result;
    }

    public FloatIterator iterator() {
        if (isEmpty()) {
            return FloatIterator.EMPTY;
        }

        return FloatIterator.of(elementData, 0, size);
    }

    public FloatStream stream() {
        return FloatStream.of(elementData, 0, size());
    }

    public FloatStream stream(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return FloatStream.of(elementData, fromIndex, toIndex);
    }

    @Override
    public <R, E extends Exception> R apply(Try.Function<? super FloatList, R, E> func) throws E {
        return func.apply(this);
    }

    @Override
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(Function<? super FloatList, R, E> func) throws E {
        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    }

    @Override
    public <E extends Exception> void accept(Try.Consumer<? super FloatList, E> action) throws E {
        action.accept(this);
    }

    @Override
    public <E extends Exception> void acceptIfNotEmpty(Try.Consumer<? super FloatList, E> action) throws E {
        if (size > 0) {
            action.accept(this);
        }
    }

    @Override
    public int hashCode() {
        return N.hashCode(elementData, 0, size);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof FloatList) {
            final FloatList other = (FloatList) obj;

            return this.size == other.size && N.equals(this.elementData, 0, other.elementData, 0, this.size);
        }

        return false;
    }

    @Override
    public String toString() {
        return size == 0 ? "[]" : N.toString(elementData, 0, size);
    }

    private void ensureCapacityInternal(int minCapacity) {
        if (elementData == N.EMPTY_FLOAT_ARRAY) {
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
        }

        ensureExplicitCapacity(minCapacity);
    }

    private void ensureExplicitCapacity(int minCapacity) {
        if (minCapacity - elementData.length > 0) {
            grow(minCapacity);
        }
    }

    private void grow(int minCapacity) {
        int oldCapacity = elementData.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);

        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }

        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            newCapacity = hugeCapacity(minCapacity);
        }

        elementData = Arrays.copyOf(elementData, newCapacity);
    }
}

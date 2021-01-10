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
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.Throwables.Function;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.Collector;
import com.landawn.abacus.util.stream.IntStream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class IntList extends PrimitiveList<Integer, int[], IntList> {

    private static final long serialVersionUID = 8661773953226671696L;

    static final Random RAND = new SecureRandom();

    private int[] elementData = N.EMPTY_INT_ARRAY;

    private int size = 0;

    public IntList() {
        super();
    }

    public IntList(int initialCapacity) {
        elementData = initialCapacity == 0 ? N.EMPTY_INT_ARRAY : new int[initialCapacity];
    }

    /**
     * The specified array is used as the element array for this list without copying action.
     *
     * @param a
     */
    public IntList(int[] a) {
        this(a, a.length);
    }

    public IntList(int[] a, int size) {
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
    public static IntList of(final int... a) {
        return new IntList(N.nullToEmpty(a));
    }

    /**
     *
     * @param a
     * @param size
     * @return
     */
    public static IntList of(final int[] a, final int size) {
        N.checkFromIndexSize(0, size, N.len(a));

        return new IntList(N.nullToEmpty(a), size);
    }

    /**
     *
     * @param a
     * @return
     */
    public static IntList copyOf(final int[] a) {
        return of(N.clone(a));
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static IntList copyOf(final int[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    //    public static IntList from(String... a) {
    //        return a == null ? new IntList() : from(a, 0, a.length);
    //    }
    //
    //    public static IntList from(String[] a, int startIndex, int endIndex) {
    //        if (a == null && (startIndex == 0 && endIndex == 0)) {
    //            return new IntList();
    //        }
    //
    //        N.checkIndex(startIndex, endIndex, a == null ? 0 : a.length);
    //
    //        final int[] elementData = new int[endIndex - startIndex];
    //
    //        for (int i = startIndex; i < endIndex; i++) {
    //            elementData[i - startIndex] = N.parseInt(a[i]);
    //        }
    //
    //        return of(elementData);
    //    }

    /**
     *
     * @param c
     * @return
     */
    public static IntList from(Collection<Integer> c) {
        if (N.isNullOrEmpty(c)) {
            return new IntList();
        }

        return from(c, 0);
    }

    /**
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static IntList from(Collection<Integer> c, int defaultForNull) {
        if (N.isNullOrEmpty(c)) {
            return new IntList();
        }

        final int[] a = new int[c.size()];
        int idx = 0;

        for (Integer e : c) {
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
    public static IntList from(final Collection<Integer> c, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isNullOrEmpty(c)) {
            return new IntList();
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
    public static IntList from(final Collection<Integer> c, final int fromIndex, final int toIndex, int defaultForNull) {
        return of(N.toIntArray(c, fromIndex, toIndex, defaultForNull));
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @return
     */
    public static IntList range(int startInclusive, final int endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @param by
     * @return
     */
    public static IntList range(int startInclusive, final int endExclusive, final int by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @return
     */
    public static IntList rangeClosed(int startInclusive, final int endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @param by
     * @return
     */
    public static IntList rangeClosed(int startInclusive, final int endInclusive, final int by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     *
     * @param element
     * @param len
     * @return
     */
    public static IntList repeat(int element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     *
     * @param len
     * @return
     */
    public static IntList random(final int len) {
        final int[] a = new int[len];

        for (int i = 0; i < len; i++) {
            a[i] = RAND.nextInt();
        }

        return of(a);
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @param len
     * @return
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
    @Override
    public int[] array() {
        return elementData;
    }

    /**
     *
     * @param index
     * @return
     */
    public int get(int index) {
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
    public int set(int index, int e) {
        rangeCheck(index);

        int oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     *
     * @param e
     */
    public void add(int e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     *
     * @param index
     * @param e
     */
    public void add(int index, int e) {
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
     * @return true, if successful
     */
    public boolean addAll(IntList c) {
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
     * @return true, if successful
     */
    public boolean addAll(int index, IntList c) {
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
     * @return true, if successful
     */
    @Override
    public boolean addAll(int[] a) {
        return addAll(size(), a);
    }

    /**
     * Adds the all.
     *
     * @param index
     * @param a
     * @return true, if successful
     */
    @Override
    public boolean addAll(int index, int[] a) {
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
    public boolean remove(int e) {
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
    public boolean removeAllOccurrences(int e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] != e) {
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
     * @return true, if successful
     */
    public boolean removeAll(IntList c) {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        return batchRemove(c, false) > 0;
    }

    /**
     * Removes the all.
     *
     * @param a
     * @return true, if successful
     */
    @Override
    public boolean removeAll(int[] a) {
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
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Exception> boolean removeIf(Throwables.IntPredicate<E> p) throws E {
        final IntList tmp = new IntList(size());

        for (int i = 0; i < size; i++) {
            if (p.test(elementData[i]) == false) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == this.size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, this.elementData, 0, tmp.size());
        N.fill(this.elementData, tmp.size(), size, 0);
        size = tmp.size;

        return true;
    }

    /**
     *
     * @param c
     * @return true, if successful
     */
    public boolean retainAll(IntList c) {
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
     * @return true, if successful
     */
    public boolean retainAll(int[] a) {
        if (N.isNullOrEmpty(a)) {
            boolean result = size() > 0;
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
    private int batchRemove(IntList c, boolean complement) {
        final int[] elementData = this.elementData;

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
    public int delete(int index) {
        rangeCheck(index);

        int oldValue = elementData[index];

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
        final int[] tmp = N.deleteAll(elementData, indices);
        N.copy(tmp, 0, elementData, 0, tmp.length);
        N.fill(elementData, tmp.length, size, 0);
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

        final int newSize = size() - (toIndex - fromIndex);

        if (toIndex < size()) {
            System.arraycopy(elementData, toIndex, elementData, fromIndex, size - toIndex);
        }

        N.fill(elementData, newSize, size(), 0);

        size = newSize;
    }

    /**
     *
     * @param oldVal
     * @param newVal
     * @return
     */
    public int replaceAll(int oldVal, int newVal) {
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
    public <E extends Exception> void replaceAll(Throwables.IntUnaryOperator<E> operator) throws E {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsInt(elementData[i]);
        }
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @param newValue
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Exception> boolean replaceIf(Throwables.IntPredicate<E> predicate, int newValue) throws E {
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
     */
    public void fill(final int fromIndex, final int toIndex, final int val) {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     *
     * @param e
     * @return true, if successful
     */
    public boolean contains(int e) {
        return indexOf(e) >= 0;
    }

    /**
     *
     * @param c
     * @return true, if successful
     */
    public boolean containsAll(IntList c) {
        if (N.isNullOrEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        final boolean isThisContainer = size() >= c.size();
        final IntList container = isThisContainer ? this : c;
        final int[] iterElements = isThisContainer ? c.array() : this.array();

        if (needToSet(size(), c.size())) {
            final Set<Integer> set = container.toSet();

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
     * @return true, if successful
     */
    @Override
    public boolean containsAll(int[] a) {
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
     * @return true, if successful
     */
    public boolean containsAny(IntList c) {
        if (this.isEmpty() || N.isNullOrEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    /**
     *
     * @param a
     * @return true, if successful
     */
    @Override
    public boolean containsAny(int[] a) {
        if (this.isEmpty() || N.isNullOrEmpty(a)) {
            return false;
        }

        return !disjoint(a);
    }

    /**
     *
     * @param c
     * @return true, if successful
     */
    public boolean disjoint(final IntList c) {
        if (isEmpty() || N.isNullOrEmpty(c)) {
            return true;
        }

        final boolean isThisContainer = size() >= c.size();
        final IntList container = isThisContainer ? this : c;
        final int[] iterElements = isThisContainer ? c.array() : this.array();

        if (needToSet(size(), c.size())) {
            final Set<Integer> set = container.toSet();

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
     * @return true, if successful
     */
    @Override
    public boolean disjoint(final int[] b) {
        if (isEmpty() || N.isNullOrEmpty(b)) {
            return true;
        }

        return disjoint(of(b));
    }

    /**
     * Returns a new list with all the elements occurred in both <code>a</code> and <code>b</code> by occurrences.
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
    public IntList intersection(final IntList b) {
        if (N.isNullOrEmpty(b)) {
            return new IntList();
        }

        final Multiset<Integer> bOccurrences = b.toMultiset();

        final IntList c = new IntList(N.min(9, size(), b.size()));

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
    public IntList intersection(final int[] a) {
        if (N.isNullOrEmpty(a)) {
            return new IntList();
        }

        return intersection(of(a));
    }

    /**
     * Returns a new list with all the elements in <code>b</code> removed by occurrences.
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
    public IntList difference(final IntList b) {
        if (N.isNullOrEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Integer> bOccurrences = b.toMultiset();

        final IntList c = new IntList(N.min(size(), N.max(9, size() - b.size())));

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
    public IntList difference(final int[] a) {
        if (N.isNullOrEmpty(a)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(a));
    }

    /**
     * <pre>
     * IntList a = IntList.of(0, 1, 2, 2, 3);
     * IntList b = IntList.of(2, 5, 1);
     * IntList c = a.symmetricDifference(b); // The elements c in a will be: [0, 2, 3, 5].
     * </pre>
     *
     * @param b
     * @return this.difference(b).addAll(b.difference(this))
     * @see IntList#difference(IntList)
     */
    public IntList symmetricDifference(final IntList b) {
        if (N.isNullOrEmpty(b)) {
            return this.copy();
        } else if (this.isEmpty()) {
            return b.copy();
        }

        final Multiset<Integer> bOccurrences = b.toMultiset();
        final IntList c = new IntList(N.max(9, Math.abs(size() - b.size())));

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
    public IntList symmetricDifference(final int[] a) {
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
    public int occurrencesOf(final int objectToFind) {
        return N.occurrencesOf(elementData, objectToFind);
    }

    /**
     *
     * @param e
     * @return
     */
    public int indexOf(int e) {
        return indexOf(0, e);
    }

    /**
     *
     * @param fromIndex
     * @param e
     * @return
     */
    public int indexOf(final int fromIndex, int e) {
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
    public int lastIndexOf(int e) {
        return lastIndexOf(size, e);
    }

    /**
     * Last index of.
     *
     * @param fromIndex the start index to traverse backwards from. Inclusive.
     * @param e
     * @return
     */
    public int lastIndexOf(final int fromIndex, int e) {
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

    public OptionalInt min() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(N.min(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public OptionalInt min(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalInt.empty() : OptionalInt.of(N.min(elementData, fromIndex, toIndex));
    }

    public OptionalInt median() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(N.median(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public OptionalInt median(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalInt.empty() : OptionalInt.of(N.median(elementData, fromIndex, toIndex));
    }

    public OptionalInt max() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(N.max(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public OptionalInt max(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalInt.empty() : OptionalInt.of(N.max(elementData, fromIndex, toIndex));
    }

    /**
     *
     * @param k
     * @return
     */
    public OptionalInt kthLargest(final int k) {
        return kthLargest(0, size(), k);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param k
     * @return
     */
    public OptionalInt kthLargest(final int fromIndex, final int toIndex, final int k) {
        checkFromToIndex(fromIndex, toIndex);
        N.checkArgPositive(k, "k");

        return toIndex - fromIndex < k ? OptionalInt.empty() : OptionalInt.of(N.kthLargest(elementData, fromIndex, toIndex, k));
    }

    public int sum() {
        return sum(0, size());
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public int sum(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return N.sum(elementData, fromIndex, toIndex);
    }

    public OptionalDouble average() {
        return average(0, size());
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public OptionalDouble average(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalDouble.empty() : OptionalDouble.of(N.average(elementData, fromIndex, toIndex));
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void forEach(Throwables.IntConsumer<E> action) throws E {
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
    public <E extends Exception> void forEach(final int fromIndex, final int toIndex, Throwables.IntConsumer<E> action) throws E {
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
    public <E extends Exception> void forEachIndexed(Throwables.IndexedIntConsumer<E> action) throws E {
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
    public <E extends Exception> void forEachIndexed(final int fromIndex, final int toIndex, Throwables.IndexedIntConsumer<E> action) throws E {
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

    public OptionalInt first() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(elementData[0]);
    }

    public OptionalInt last() {
        return size() == 0 ? OptionalInt.empty() : OptionalInt.of(elementData[size() - 1]);
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> OptionalInt findFirst(Throwables.IntPredicate<E> predicate) throws E {
        for (int i = 0; i < size; i++) {
            if (predicate.test(elementData[i])) {
                return OptionalInt.of(elementData[i]);
            }
        }

        return OptionalInt.empty();
    }

    /**
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> OptionalInt findLast(Throwables.IntPredicate<E> predicate) throws E {
        for (int i = size - 1; i >= 0; i--) {
            if (predicate.test(elementData[i])) {
                return OptionalInt.of(elementData[i]);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Find first index.
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> OptionalInt findFirstIndex(Throwables.IntPredicate<E> predicate) throws E {
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
    public <E extends Exception> OptionalInt findLastIndex(Throwables.IntPredicate<E> predicate) throws E {
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
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Exception> boolean allMatch(Throwables.IntPredicate<E> filter) throws E {
        return allMatch(0, size(), filter);
    }

    /**
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Exception> boolean allMatch(final int fromIndex, final int toIndex, Throwables.IntPredicate<E> filter) throws E {
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
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Exception> boolean anyMatch(Throwables.IntPredicate<E> filter) throws E {
        return anyMatch(0, size(), filter);
    }

    /**
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Exception> boolean anyMatch(final int fromIndex, final int toIndex, Throwables.IntPredicate<E> filter) throws E {
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
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Exception> boolean noneMatch(Throwables.IntPredicate<E> filter) throws E {
        return noneMatch(0, size(), filter);
    }

    /**
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Exception> boolean noneMatch(final int fromIndex, final int toIndex, Throwables.IntPredicate<E> filter) throws E {
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
    public <E extends Exception> int count(Throwables.IntPredicate<E> filter) throws E {
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
    public <E extends Exception> int count(final int fromIndex, final int toIndex, Throwables.IntPredicate<E> filter) throws E {
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
    public <E extends Exception> IntList filter(Throwables.IntPredicate<E> filter) throws E {
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
    public <E extends Exception> IntList filter(final int fromIndex, final int toIndex, Throwables.IntPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex);

        return IntList.of(N.filter(elementData, fromIndex, toIndex, filter));
    }

    /**
     *
     * @param <E>
     * @param filter
     * @param max
     * @return a new List with the elements match the provided predicate.
     * @throws E the e
     */
    public <E extends Exception> IntList filter(Throwables.IntPredicate<E> filter, int max) throws E {
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
    public <E extends Exception> IntList filter(final int fromIndex, final int toIndex, Throwables.IntPredicate<E> filter, final int max) throws E {
        checkFromToIndex(fromIndex, toIndex);

        return IntList.of(N.filter(elementData, fromIndex, toIndex, filter, max));
    }

    /**
     *
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <E extends Exception> IntList map(final Throwables.IntUnaryOperator<E> mapper) throws E {
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
    public <E extends Exception> IntList map(final int fromIndex, final int toIndex, final Throwables.IntUnaryOperator<E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex);

        final IntList result = new IntList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(mapper.applyAsInt(elementData[i]));
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
    public <T, E extends Exception> List<T> mapToObj(final Throwables.IntFunction<? extends T, E> mapper) throws E {
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
    public <T, E extends Exception> List<T> mapToObj(final int fromIndex, final int toIndex, final Throwables.IntFunction<? extends T, E> mapper) throws E {
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
     *        return OptionalInt.empty();
     *    }
     *
     *    int result = elementData[0];
     *
     *    for (int i = 1; i < size; i++) {
     *        result = accumulator.applyAsInt(result, elementData[i]);
     *    }
     *
     *    return OptionalInt.of(result);
     * </code>
     * </pre>
     *
     * @param <E>
     * @param accumulator
     * @return
     * @throws E the e
     */
    public <E extends Exception> OptionalInt reduce(final Throwables.IntBinaryOperator<E> accumulator) throws E {
        if (isEmpty()) {
            return OptionalInt.empty();
        }

        int result = elementData[0];

        for (int i = 1; i < size; i++) {
            result = accumulator.applyAsInt(result, elementData[i]);
        }

        return OptionalInt.of(result);
    }

    /**
     * This is equivalent to:
     * <pre>
     * <code>
     *     if (isEmpty()) {
     *         return identity;
     *     }
     *
     *     int result = identity;
     *
     *     for (int i = 0; i < size; i++) {
     *         result = accumulator.applyAsInt(result, elementData[i]);
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
    public <E extends Exception> int reduce(final int identity, final Throwables.IntBinaryOperator<E> accumulator) throws E {
        if (isEmpty()) {
            return identity;
        }

        int result = identity;

        for (int i = 0; i < size; i++) {
            result = accumulator.applyAsInt(result, elementData[i]);
        }

        return result;
    }

    /**
     * Checks for duplicates.
     *
     * @return true, if successful
     */
    @Override
    public boolean hasDuplicates() {
        return N.hasDuplicates(elementData, 0, size, false);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    @Override
    public IntList distinct(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            return of(N.distinct(elementData, fromIndex, toIndex));
        } else {
            return of(N.copyOfRange(elementData, fromIndex, toIndex));
        }
    }

    /**
     *
     * @param n
     * @return
     */
    public IntList top(final int n) {
        return top(0, size(), n);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param n
     * @return
     */
    public IntList top(final int fromIndex, final int toIndex, final int n) {
        checkFromToIndex(fromIndex, toIndex);

        return of(N.top(elementData, fromIndex, toIndex, n));
    }

    /**
     *
     * @param n
     * @param cmp
     * @return
     */
    public IntList top(final int n, Comparator<? super Integer> cmp) {
        return top(0, size(), n, cmp);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param cmp
     * @return
     */
    public IntList top(final int fromIndex, final int toIndex, final int n, Comparator<? super Integer> cmp) {
        checkFromToIndex(fromIndex, toIndex);

        return of(N.top(elementData, fromIndex, toIndex, n, cmp));
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
    public int binarySearch(final int key) {
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
    public int binarySearch(final int fromIndex, final int toIndex, final int key) {
        checkFromToIndex(fromIndex, toIndex);

        return N.binarySearch(elementData, fromIndex, toIndex, key);
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
    public IntList copy() {
        return new IntList(N.copyOfRange(elementData, 0, size));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    @Override
    public IntList copy(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return new IntList(N.copyOfRange(elementData, fromIndex, toIndex));
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
    public IntList copy(final int fromIndex, final int toIndex, final int step) {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex);

        return new IntList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Returns List of {@code IntList} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     *
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    @Override
    public List<IntList> split(final int fromIndex, final int toIndex, final int chunkSize) {
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

    public List<Integer> boxed() {
        return boxed(0, size);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public List<Integer> boxed(int fromIndex, int toIndex) {
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
            a[i] = elementData[i];
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
            a[i] = elementData[i];
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
            a[i] = elementData[i];
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
     */
    @Override
    public <C extends Collection<Integer>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier) {
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
    public Multiset<Integer> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Integer>> supplier) {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Integer> multiset = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            multiset.add(elementData[i]);
        }

        return multiset;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param <E2>
     * @param keyMapper
     * @param valueMapper
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.IntFunction<? extends K, E> keyMapper,
            Throwables.IntFunction<? extends V, E2> valueMapper) throws E, E2 {
        return toMap(keyMapper, valueMapper, Factory.<K, V> ofMap());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param <E>
     * @param <E2>
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.IntFunction<? extends K, E> keyMapper,
            Throwables.IntFunction<? extends V, E2> valueMapper, IntFunction<? extends M> mapFactory) throws E, E2 {
        final Throwables.BinaryOperator<V, RuntimeException> mergeFunction = Fn.throwingMerger();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param <E2>
     * @param <E3>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     */
    public <K, V, E extends Exception, E2 extends Exception, E3 extends Exception> Map<K, V> toMap(Throwables.IntFunction<? extends K, E> keyMapper,
            Throwables.IntFunction<? extends V, E2> valueMapper, Throwables.BinaryOperator<V, E3> mergeFunction) throws E, E2, E3 {
        return toMap(keyMapper, valueMapper, mergeFunction, Factory.<K, V> ofMap());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param <E>
     * @param <E2>
     * @param <E3>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     */
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception, E3 extends Exception> M toMap(
            Throwables.IntFunction<? extends K, E> keyMapper, Throwables.IntFunction<? extends V, E2> valueMapper,
            Throwables.BinaryOperator<V, E3> mergeFunction, IntFunction<? extends M> mapFactory) throws E, E2, E3 {
        final M result = mapFactory.apply(size);

        for (int i = 0; i < size; i++) {
            Maps.merge(result, keyMapper.apply(elementData[i]), valueMapper.apply(elementData[i]), mergeFunction);
        }

        return result;
    }

    /**
     *
     * @param <K> the key type
     * @param <A>
     * @param <D>
     * @param <E>
     * @param keyMapper
     * @param downstream
     * @return
     * @throws E the e
     */
    public <K, A, D, E extends Exception> Map<K, D> toMap(Throwables.IntFunction<? extends K, E> keyMapper, Collector<Integer, A, D> downstream) throws E {
        return toMap(keyMapper, downstream, Factory.<K, D> ofMap());
    }

    /**
     *
     * @param <K> the key type
     * @param <A>
     * @param <D>
     * @param <M>
     * @param <E>
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @throws E the e
     */
    public <K, A, D, M extends Map<K, D>, E extends Exception> M toMap(final Throwables.IntFunction<? extends K, E> keyMapper,
            final Collector<Integer, A, D> downstream, final IntFunction<? extends M> mapFactory) throws E {
        final M result = mapFactory.apply(size);
        final Supplier<A> downstreamSupplier = downstream.supplier();
        final BiConsumer<A, Integer> downstreamAccumulator = downstream.accumulator();
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
     */
    public IntStream stream(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return IntStream.of(elementData, fromIndex, toIndex);
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
    public <R, E extends Exception> R apply(Throwables.Function<? super IntList, R, E> func) throws E {
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
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(Function<? super IntList, R, E> func) throws E {
        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void accept(Throwables.Consumer<? super IntList, E> action) throws E {
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
    public <E extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super IntList, E> action) throws E {
        return If.is(size > 0).then(this, action);
    }

    @Override
    public int hashCode() {
        return N.hashCode(elementData, 0, size);
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof IntList) {
            final IntList other = (IntList) obj;

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
            elementData = new int[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}

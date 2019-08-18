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
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.Try.Function;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.CharStream;
import com.landawn.abacus.util.stream.Collector;

// TODO: Auto-generated Javadoc
/**
 * The Class CharList.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class CharList extends PrimitiveList<Character, char[], CharList> {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 7293826835233022514L;

    /** The Constant RAND. */
    static final Random RAND = new SecureRandom();

    /** The element data. */
    private char[] elementData = N.EMPTY_CHAR_ARRAY;

    /** The size. */
    private int size = 0;

    /**
     * Instantiates a new char list.
     */
    public CharList() {
        super();
    }

    /**
     * Instantiates a new char list.
     *
     * @param initialCapacity
     */
    public CharList(int initialCapacity) {
        elementData = initialCapacity == 0 ? N.EMPTY_CHAR_ARRAY : new char[initialCapacity];
    }

    /**
     * The specified array is used as the element array for this list without copying action.
     *
     * @param a
     */
    public CharList(char[] a) {
        this(a, a.length);
    }

    /**
     * Instantiates a new char list.
     *
     * @param a
     * @param size
     */
    public CharList(char[] a, int size) {
        N.checkFromIndexSize(0, size, a.length);

        this.elementData = a;
        this.size = size;
    }

    /**
     * Of.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static CharList of(final char... a) {
        return new CharList(N.nullToEmpty(a));
    }

    /**
     * Of.
     *
     * @param a
     * @param size
     * @return
     */
    public static CharList of(final char[] a, final int size) {
        N.checkFromIndexSize(0, size, N.len(a));

        return new CharList(N.nullToEmpty(a), size);
    }

    /**
     * Copy of.
     *
     * @param a
     * @return
     */
    public static CharList copyOf(final char[] a) {
        return of(N.clone(a));
    }

    /**
     * Copy of.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static CharList copyOf(final char[] a, final int fromIndex, final int toIndex) {
        return of(N.copyOfRange(a, fromIndex, toIndex));
    }

    /**
     * From.
     *
     * @param c
     * @return
     */
    public static CharList from(Collection<Character> c) {
        if (N.isNullOrEmpty(c)) {
            return new CharList();
        }

        return from(c, (char) 0);
    }

    /**
     * From.
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static CharList from(Collection<Character> c, char defaultForNull) {
        if (N.isNullOrEmpty(c)) {
            return new CharList();
        }

        final char[] a = new char[c.size()];
        int idx = 0;

        for (Character e : c) {
            a[idx++] = e == null ? defaultForNull : e;
        }

        return of(a);
    }

    /**
     * From.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static CharList from(final Collection<Character> c, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isNullOrEmpty(c)) {
            return new CharList();
        }

        return from(c, fromIndex, toIndex, (char) 0);
    }

    /**
     * From.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param defaultForNull
     * @return
     */
    public static CharList from(final Collection<Character> c, final int fromIndex, final int toIndex, char defaultForNull) {
        return of(N.toCharArray(c, fromIndex, toIndex, defaultForNull));
    }

    /**
     * Range.
     *
     * @param startInclusive
     * @param endExclusive
     * @return
     */
    public static CharList range(char startInclusive, final char endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     * Range.
     *
     * @param startInclusive
     * @param endExclusive
     * @param by
     * @return
     */
    public static CharList range(char startInclusive, final char endExclusive, final int by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     * Range closed.
     *
     * @param startInclusive
     * @param endInclusive
     * @return
     */
    public static CharList rangeClosed(char startInclusive, final char endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     * Range closed.
     *
     * @param startInclusive
     * @param endInclusive
     * @param by
     * @return
     */
    public static CharList rangeClosed(char startInclusive, final char endInclusive, final int by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     * Repeat.
     *
     * @param element
     * @param len
     * @return
     */
    public static CharList repeat(char element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
     * Random.
     *
     * @param len
     * @return
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
     * Random.
     *
     * @param startInclusive
     * @param endExclusive
     * @param len
     * @return
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
     * Random.
     *
     * @param candicates
     * @param len
     * @return
     */
    public static CharList random(final char[] candicates, final int len) {
        if (N.isNullOrEmpty(candicates) || candicates.length >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        } else if (candicates.length == 1) {
            return repeat(candicates[0], len);
        }

        final int n = candicates.length;
        final char[] a = new char[len];

        for (int i = 0; i < len; i++) {
            a[i] = candicates[RAND.nextInt(n)];
        }

        return of(a);
    }

    /**
     * Returns the original element array without copying.
     *
     * @return
     */
    @Override
    public char[] array() {
        return elementData;
    }

    /**
     * Gets the.
     *
     * @param index
     * @return
     */
    public char get(int index) {
        rangeCheck(index);

        return elementData[index];
    }

    /**
     * Range check.
     *
     * @param index
     */
    private void rangeCheck(int index) {
        if (index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    /**
     * Sets the.
     *
     * @param index
     * @param e
     * @return
     */
    public char set(int index, char e) {
        rangeCheck(index);

        char oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     * Adds the.
     *
     * @param e
     */
    public void add(char e) {
        ensureCapacityInternal(size + 1);

        elementData[size++] = e;
    }

    /**
     * Adds the.
     *
     * @param index
     * @param e
     */
    public void add(int index, char e) {
        rangeCheckForAdd(index);

        ensureCapacityInternal(size + 1);

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
    public boolean addAll(CharList c) {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        int numNew = c.size();

        ensureCapacityInternal(size + numNew);

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
    public boolean addAll(int index, CharList c) {
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

    /**
     * Adds the all.
     *
     * @param a
     * @return true, if successful
     */
    @Override
    public boolean addAll(char[] a) {
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
    public boolean addAll(int index, char[] a) {
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
     * Removes the.
     *
     * @param e
     * @return <tt>true</tt> if this list contained the specified element
     */
    public boolean remove(char e) {
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
    public boolean removeAllOccurrences(char e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] != e) {
                elementData[w++] = elementData[i];
            }
        }

        int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, (char) 0);

            size = w;
        }

        return numRemoved > 0;
    }

    /**
     * Fast remove.
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
    public boolean removeAll(CharList c) {
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
    public boolean removeAll(char[] a) {
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
    public <E extends Exception> boolean removeIf(Try.CharPredicate<E> p) throws E {
        final CharList tmp = new CharList(size());

        for (int i = 0; i < size; i++) {
            if (p.test(elementData[i]) == false) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == this.size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, this.elementData, 0, tmp.size());
        N.fill(this.elementData, tmp.size(), size, (char) 0);
        size = tmp.size;

        return true;
    }

    /**
     * Retain all.
     *
     * @param c
     * @return true, if successful
     */
    public boolean retainAll(CharList c) {
        if (N.isNullOrEmpty(c)) {
            boolean result = size() > 0;
            clear();
            return result;
        }

        return batchRemove(c, true) > 0;
    }

    /**
     * Retain all.
     *
     * @param a
     * @return true, if successful
     */
    public boolean retainAll(char[] a) {
        if (N.isNullOrEmpty(a)) {
            boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(CharList.of(a));
    }

    /**
     * Batch remove.
     *
     * @param c
     * @param complement
     * @return
     */
    private int batchRemove(CharList c, boolean complement) {
        final char[] elementData = this.elementData;

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

        int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, (char) 0);

            size = w;
        }

        return numRemoved;
    }

    /**
     * Delete.
     *
     * @param index
     * @return
     */
    public char delete(int index) {
        rangeCheck(index);

        char oldValue = elementData[index];

        fastRemove(index);

        return oldValue;
    }

    /**
     * Delete all.
     *
     * @param indices
     */
    @Override
    @SafeVarargs
    public final void deleteAll(int... indices) {
        final char[] tmp = N.deleteAll(elementData, indices);
        N.copy(tmp, 0, elementData, 0, tmp.length);
        N.fill(elementData, tmp.length, size, (char) 0);
        size = tmp.length;
    }

    /**
     * Delete range.
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

        N.fill(elementData, newSize, size(), (char) 0);

        size = newSize;
    }

    /**
     * Replace all.
     *
     * @param oldVal
     * @param newVal
     * @return
     */
    public int replaceAll(char oldVal, char newVal) {
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
     * Replace all.
     *
     * @param <E>
     * @param operator
     * @throws E the e
     */
    public <E extends Exception> void replaceAll(Try.CharUnaryOperator<E> operator) throws E {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsChar(elementData[i]);
        }
    }

    /**
     * Replace if.
     *
     * @param <E>
     * @param predicate
     * @param newValue
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Exception> boolean replaceIf(Try.CharPredicate<E> predicate, char newValue) throws E {
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
     * Fill.
     *
     * @param val
     */
    public void fill(final char val) {
        fill(0, size(), val);
    }

    /**
     * Fill.
     *
     * @param fromIndex
     * @param toIndex
     * @param val
     */
    public void fill(final int fromIndex, final int toIndex, final char val) {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     * Contains.
     *
     * @param e
     * @return true, if successful
     */
    public boolean contains(char e) {
        return indexOf(e) >= 0;
    }

    /**
     * Contains all.
     *
     * @param c
     * @return true, if successful
     */
    public boolean containsAll(CharList c) {
        if (N.isNullOrEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        final boolean isThisContainer = size() >= c.size();
        final CharList container = isThisContainer ? this : c;
        final char[] iterElements = isThisContainer ? c.array() : this.array();

        if (needToSet(size(), c.size())) {
            final Set<Character> set = container.toSet();

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
     * Contains all.
     *
     * @param a
     * @return true, if successful
     */
    @Override
    public boolean containsAll(char[] a) {
        if (N.isNullOrEmpty(a)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        return containsAll(of(a));
    }

    /**
     * Contains any.
     *
     * @param c
     * @return true, if successful
     */
    public boolean containsAny(CharList c) {
        if (this.isEmpty() || N.isNullOrEmpty(c)) {
            return false;
        }

        return !disjoint(c);
    }

    /**
     * Contains any.
     *
     * @param a
     * @return true, if successful
     */
    @Override
    public boolean containsAny(char[] a) {
        if (this.isEmpty() || N.isNullOrEmpty(a)) {
            return false;
        }

        return !disjoint(a);
    }

    /**
     * Disjoint.
     *
     * @param c
     * @return true, if successful
     */
    public boolean disjoint(final CharList c) {
        if (isEmpty() || N.isNullOrEmpty(c)) {
            return true;
        }

        final boolean isThisContainer = size() >= c.size();
        final CharList container = isThisContainer ? this : c;
        final char[] iterElements = isThisContainer ? c.array() : this.array();

        if (needToSet(size(), c.size())) {
            final Set<Character> set = container.toSet();

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
     * Disjoint.
     *
     * @param b
     * @return true, if successful
     */
    @Override
    public boolean disjoint(final char[] b) {
        if (isEmpty() || N.isNullOrEmpty(b)) {
            return true;
        }

        return disjoint(of(b));
    }

    /**
     * Intersection.
     *
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public CharList intersection(final CharList b) {
        if (N.isNullOrEmpty(b)) {
            return new CharList();
        }

        final Multiset<Character> bOccurrences = b.toMultiset();

        final CharList c = new CharList(N.min(9, size(), b.size()));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.getAndRemove(elementData[i]) > 0) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Intersection.
     *
     * @param a
     * @return
     */
    public CharList intersection(final char[] a) {
        if (N.isNullOrEmpty(a)) {
            return new CharList();
        }

        return intersection(of(a));
    }

    /**
     * Difference.
     *
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public CharList difference(CharList b) {
        if (N.isNullOrEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Character> bOccurrences = b.toMultiset();

        final CharList c = new CharList(N.min(size(), N.max(9, size() - b.size())));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.getAndRemove(elementData[i]) < 1) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Difference.
     *
     * @param a
     * @return
     */
    public CharList difference(final char[] a) {
        if (N.isNullOrEmpty(a)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        return difference(of(a));
    }

    /**
     * Symmetric difference.
     *
     * @param b
     * @return this.difference(b).addAll(b.difference(this))
     * @see IntList#symmetricDifference(IntList)
     */
    public CharList symmetricDifference(CharList b) {
        if (N.isNullOrEmpty(b)) {
            return this.copy();
        } else if (this.isEmpty()) {
            return b.copy();
        }

        final Multiset<Character> bOccurrences = b.toMultiset();
        final CharList c = new CharList(N.max(9, Math.abs(size() - b.size())));

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
     * Symmetric difference.
     *
     * @param a
     * @return
     */
    public CharList symmetricDifference(final char[] a) {
        if (N.isNullOrEmpty(a)) {
            return of(N.copyOfRange(elementData, 0, size()));
        } else if (this.isEmpty()) {
            return of(N.copyOfRange(a, 0, a.length));
        }

        return symmetricDifference(of(a));
    }

    /**
     * Occurrences of.
     *
     * @param objectToFind
     * @return
     */
    public int occurrencesOf(final char objectToFind) {
        return N.occurrencesOf(elementData, objectToFind);
    }

    /**
     * Index of.
     *
     * @param e
     * @return
     */
    public int indexOf(char e) {
        return indexOf(0, e);
    }

    /**
     * Index of.
     *
     * @param fromIndex
     * @param e
     * @return
     */
    public int indexOf(final int fromIndex, char e) {
        checkFromToIndex(fromIndex, size);

        for (int i = fromIndex; i < size; i++) {
            if (elementData[i] == e) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Last index of.
     *
     * @param e
     * @return
     */
    public int lastIndexOf(char e) {
        return lastIndexOf(size, e);
    }

    /**
     * Last index of.
     *
     * @param fromIndex the start index to traverse backwards from. Inclusive.
     * @param e
     * @return
     */
    public int lastIndexOf(final int fromIndex, char e) {
        checkFromToIndex(0, fromIndex);

        for (int i = fromIndex == size ? size - 1 : fromIndex; i >= 0; i--) {
            if (elementData[i] == e) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Min.
     *
     * @return
     */
    public OptionalChar min() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(N.min(elementData, 0, size));
    }

    /**
     * Min.
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public OptionalChar min(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalChar.empty() : OptionalChar.of(N.min(elementData, fromIndex, toIndex));
    }

    /**
     * Median.
     *
     * @return
     */
    public OptionalChar median() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(N.median(elementData, 0, size));
    }

    /**
     * Median.
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public OptionalChar median(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalChar.empty() : OptionalChar.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     * Max.
     *
     * @return
     */
    public OptionalChar max() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(N.max(elementData, 0, size));
    }

    /**
     * Max.
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public OptionalChar max(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalChar.empty() : OptionalChar.of(N.max(elementData, fromIndex, toIndex));
    }

    /**
     * Kth largest.
     *
     * @param k
     * @return
     */
    public OptionalChar kthLargest(final int k) {
        return kthLargest(0, size(), k);
    }

    /**
     * Kth largest.
     *
     * @param fromIndex
     * @param toIndex
     * @param k
     * @return
     */
    public OptionalChar kthLargest(final int fromIndex, final int toIndex, final int k) {
        checkFromToIndex(fromIndex, toIndex);
        N.checkArgPositive(k, "k");

        return toIndex - fromIndex < k ? OptionalChar.empty() : OptionalChar.of(N.kthLargest(elementData, fromIndex, toIndex, k));
    }

    /**
     * Sum.
     *
     * @return
     */
    public int sum() {
        return sum(0, size());
    }

    /**
     * Sum.
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public int sum(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return N.sum(elementData, fromIndex, toIndex);
    }

    /**
     * Average.
     *
     * @return
     */
    public OptionalDouble average() {
        return average(0, size());
    }

    /**
     * Average.
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
     * For each.
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void forEach(Try.CharConsumer<E> action) throws E {
        forEach(0, size, action);
    }

    /**
     * For each.
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param action
     * @throws E the e
     */
    public <E extends Exception> void forEach(final int fromIndex, final int toIndex, Try.CharConsumer<E> action) throws E {
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
     * First.
     *
     * @return
     */
    public OptionalChar first() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(elementData[0]);
    }

    /**
     * Last.
     *
     * @return
     */
    public OptionalChar last() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(elementData[size() - 1]);
    }

    /**
     * Find first.
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> OptionalChar findFirst(Try.CharPredicate<E> predicate) throws E {
        for (int i = 0; i < size; i++) {
            if (predicate.test(elementData[i])) {
                return OptionalChar.of(elementData[i]);
            }
        }

        return OptionalChar.empty();
    }

    /**
     * Find last.
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> OptionalChar findLast(Try.CharPredicate<E> predicate) throws E {
        for (int i = size - 1; i >= 0; i--) {
            if (predicate.test(elementData[i])) {
                return OptionalChar.of(elementData[i]);
            }
        }

        return OptionalChar.empty();
    }

    /**
     * Find first index.
     *
     * @param <E>
     * @param predicate
     * @return
     * @throws E the e
     */
    public <E extends Exception> OptionalInt findFirstIndex(Try.CharPredicate<E> predicate) throws E {
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
    public <E extends Exception> OptionalInt findLastIndex(Try.CharPredicate<E> predicate) throws E {
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
    public <E extends Exception> boolean allMatch(Try.CharPredicate<E> filter) throws E {
        return allMatch(0, size(), filter);
    }

    /**
     * All match.
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Exception> boolean allMatch(final int fromIndex, final int toIndex, Try.CharPredicate<E> filter) throws E {
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
    public <E extends Exception> boolean anyMatch(Try.CharPredicate<E> filter) throws E {
        return anyMatch(0, size(), filter);
    }

    /**
     * Any match.
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Exception> boolean anyMatch(final int fromIndex, final int toIndex, Try.CharPredicate<E> filter) throws E {
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
    public <E extends Exception> boolean noneMatch(Try.CharPredicate<E> filter) throws E {
        return noneMatch(0, size(), filter);
    }

    /**
     * None match.
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return true, if successful
     * @throws E the e
     */
    public <E extends Exception> boolean noneMatch(final int fromIndex, final int toIndex, Try.CharPredicate<E> filter) throws E {
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
     * Count.
     *
     * @param <E>
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> int count(Try.CharPredicate<E> filter) throws E {
        return count(0, size(), filter);
    }

    /**
     * Count.
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> int count(final int fromIndex, final int toIndex, Try.CharPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex);

        return N.count(elementData, fromIndex, toIndex, filter);
    }

    /**
     * Filter.
     *
     * @param <E>
     * @param filter
     * @return a new List with the elements match the provided predicate.
     * @throws E the e
     */
    public <E extends Exception> CharList filter(Try.CharPredicate<E> filter) throws E {
        return filter(0, size(), filter);
    }

    /**
     * Filter.
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public <E extends Exception> CharList filter(final int fromIndex, final int toIndex, Try.CharPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex);

        return N.filter(elementData, fromIndex, toIndex, filter);
    }

    /**
     * Filter.
     *
     * @param <E>
     * @param filter
     * @param max
     * @return a new List with the elements match the provided predicate.
     * @throws E the e
     */
    public <E extends Exception> CharList filter(Try.CharPredicate<E> filter, int max) throws E {
        return filter(0, size(), filter, max);
    }

    /**
     * Filter.
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public <E extends Exception> CharList filter(final int fromIndex, final int toIndex, Try.CharPredicate<E> filter, final int max) throws E {
        checkFromToIndex(fromIndex, toIndex);

        return N.filter(elementData, fromIndex, toIndex, filter, max);
    }

    /**
     * Map.
     *
     * @param <E>
     * @param mapper
     * @return
     * @throws E the e
     */
    public <E extends Exception> CharList map(final Try.CharUnaryOperator<E> mapper) throws E {
        return map(0, size, mapper);
    }

    /**
     * Map.
     *
     * @param <E>
     * @param fromIndex
     * @param toIndex
     * @param mapper
     * @return
     * @throws E the e
     */
    public <E extends Exception> CharList map(final int fromIndex, final int toIndex, final Try.CharUnaryOperator<E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex);

        final CharList result = new CharList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(mapper.applyAsChar(elementData[i]));
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
    public <T, E extends Exception> List<T> mapToObj(final Try.CharFunction<? extends T, E> mapper) throws E {
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
    public <T, E extends Exception> List<T> mapToObj(final int fromIndex, final int toIndex, final Try.CharFunction<? extends T, E> mapper) throws E {
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
     *        return OptionalChar.empty();
     *    }
     * 
     *    char result = elementData[0];
     * 
     *    for (int i = 1; i < size; i++) {
     *        result = accumulator.applyAsChar(result, elementData[i]);
     *    }
     * 
     *    return OptionalChar.of(result);
     * </code>
     * </pre>
     *
     * @param <E>
     * @param accumulator
     * @return
     * @throws E the e
     */
    public <E extends Exception> OptionalChar reduce(final Try.CharBinaryOperator<E> accumulator) throws E {
        if (isEmpty()) {
            return OptionalChar.empty();
        }

        char result = elementData[0];

        for (int i = 1; i < size; i++) {
            result = accumulator.applyAsChar(result, elementData[i]);
        }

        return OptionalChar.of(result);
    }

    /**
     * This is equivalent to:
     * <pre>
     * <code>
     *     if (isEmpty()) {
     *         return identity;
     *     }
     * 
     *     char result = identity;
     * 
     *     for (int i = 0; i < size; i++) {
     *         result = accumulator.applyAsChar(result, elementData[i]);
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
    public <E extends Exception> char reduce(final char identity, final Try.CharBinaryOperator<E> accumulator) throws E {
        if (isEmpty()) {
            return identity;
        }

        char result = identity;

        for (int i = 0; i < size; i++) {
            result = accumulator.applyAsChar(result, elementData[i]);
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
     * Distinct.
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    @Override
    public CharList distinct(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        if (toIndex - fromIndex > 1) {
            return of(N.distinct(elementData, fromIndex, toIndex));
        } else {
            return of(N.copyOfRange(elementData, fromIndex, toIndex));
        }
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
    public int binarySearch(final char key) {
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
    public int binarySearch(final int fromIndex, final int toIndex, final char key) {
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
     * Reverse.
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
     * Rotate.
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
     * Shuffle.
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
     * Swap.
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
     * Copy.
     *
     * @return
     */
    @Override
    public CharList copy() {
        return new CharList(N.copyOfRange(elementData, 0, size));
    }

    /**
     * Copy.
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    @Override
    public CharList copy(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return new CharList(N.copyOfRange(elementData, fromIndex, toIndex));
    }

    /**
     * Copy.
     *
     * @param from
     * @param to
     * @param step
     * @return
     * @see N#copyOfRange(int[], int, int, int)
     */
    @Override
    public CharList copy(final int from, final int to, final int step) {
        checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from);

        return new CharList(N.copyOfRange(elementData, from, to, step));
    }

    /**
     * Returns List of {@code CharList} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *  
     *
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    @Override
    public List<CharList> split(final int fromIndex, final int toIndex, final int chunkSize) {
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
     * Join.
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
     * Join.
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
    public CharList trimToSize() {
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
            N.fill(elementData, 0, size, (char) 0);
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
     * Size.
     *
     * @return
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Boxed.
     *
     * @return
     */
    public List<Character> boxed() {
        return boxed(0, size);
    }

    /**
     * Boxed.
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public List<Character> boxed(int fromIndex, int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        final List<Character> res = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            res.add(elementData[i]);
        }

        return res;
    }

    /**
     * To array.
     *
     * @return
     */
    @Override
    public char[] toArray() {
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
            a[i] = elementData[i];
        }

        return IntList.of(a);
    }

    /**
     * To collection.
     *
     * @param <C>
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    @Override
    public <C extends Collection<Character>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier) {
        checkFromToIndex(fromIndex, toIndex);

        final C c = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            c.add(elementData[i]);
        }

        return c;
    }

    /**
     * To multiset.
     *
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    @Override
    public Multiset<Character> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Character>> supplier) {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Character> multiset = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            multiset.add(elementData[i]);
        }

        return multiset;
    }

    /**
     * To map.
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
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Try.CharFunction<? extends K, E> keyMapper,
            Try.CharFunction<? extends V, E2> valueMapper) throws E, E2 {
        return toMap(keyMapper, valueMapper, Factory.<K, V> ofMap());
    }

    /**
     * To map.
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
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Try.CharFunction<? extends K, E> keyMapper,
            Try.CharFunction<? extends V, E2> valueMapper, IntFunction<? extends M> mapFactory) throws E, E2 {
        final Try.BinaryOperator<V, RuntimeException> mergeFunction = Fn.throwingMerger();

        return toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     * To map.
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
    public <K, V, E extends Exception, E2 extends Exception, E3 extends Exception> Map<K, V> toMap(Try.CharFunction<? extends K, E> keyMapper,
            Try.CharFunction<? extends V, E2> valueMapper, Try.BinaryOperator<V, E3> mergeFunction) throws E, E2, E3 {
        return toMap(keyMapper, valueMapper, mergeFunction, Factory.<K, V> ofMap());
    }

    /**
     * To map.
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
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception, E3 extends Exception> M toMap(Try.CharFunction<? extends K, E> keyMapper,
            Try.CharFunction<? extends V, E2> valueMapper, Try.BinaryOperator<V, E3> mergeFunction, IntFunction<? extends M> mapFactory) throws E, E2, E3 {
        final M result = mapFactory.apply(size);

        for (int i = 0; i < size; i++) {
            Maps.merge(result, keyMapper.apply(elementData[i]), valueMapper.apply(elementData[i]), mergeFunction);
        }

        return result;
    }

    /**
     * To map.
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
    public <K, A, D, E extends Exception> Map<K, D> toMap(Try.CharFunction<? extends K, E> keyMapper, Collector<Character, A, D> downstream) throws E {
        return toMap(keyMapper, downstream, Factory.<K, D> ofMap());
    }

    /**
     * To map.
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
    public <K, A, D, M extends Map<K, D>, E extends Exception> M toMap(final Try.CharFunction<? extends K, E> keyMapper,
            final Collector<Character, A, D> downstream, final IntFunction<? extends M> mapFactory) throws E {
        final M result = mapFactory.apply(size);
        final Supplier<A> downstreamSupplier = downstream.supplier();
        final BiConsumer<A, Character> downstreamAccumulator = downstream.accumulator();
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

    /**
     * Iterator.
     *
     * @return
     */
    public CharIterator iterator() {
        if (isEmpty()) {
            return CharIterator.EMPTY;
        }

        return CharIterator.of(elementData, 0, size);
    }

    /**
     * Stream.
     *
     * @return
     */
    public CharStream stream() {
        return CharStream.of(elementData, 0, size());
    }

    /**
     * Stream.
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public CharStream stream(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        return CharStream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Apply.
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    @Override
    public <R, E extends Exception> R apply(Try.Function<? super CharList, R, E> func) throws E {
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
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(Function<? super CharList, R, E> func) throws E {
        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     * Accept.
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void accept(Try.Consumer<? super CharList, E> action) throws E {
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
    public <E extends Exception> void acceptIfNotEmpty(Try.Consumer<? super CharList, E> action) throws E {
        if (size > 0) {
            action.accept(this);
        }
    }

    /**
     * Hash code.
     *
     * @return
     */
    @Override
    public int hashCode() {
        return N.hashCode(elementData, 0, size);
    }

    /**
     * Equals.
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof CharList) {
            final CharList other = (CharList) obj;

            return this.size == other.size && N.equals(this.elementData, 0, other.elementData, 0, this.size);
        }

        return false;
    }

    /**
     * To string.
     *
     * @return
     */
    @Override
    public String toString() {
        return size == 0 ? "[]" : N.toString(elementData, 0, size);
    }

    /**
     * Ensure capacity internal.
     *
     * @param minCapacity
     */
    private void ensureCapacityInternal(int minCapacity) {
        if (elementData == N.EMPTY_CHAR_ARRAY) {
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
        }

        ensureExplicitCapacity(minCapacity);
    }

    /**
     * Ensure explicit capacity.
     *
     * @param minCapacity
     */
    private void ensureExplicitCapacity(int minCapacity) {
        if (minCapacity - elementData.length > 0) {
            grow(minCapacity);
        }
    }

    /**
     * Grow.
     *
     * @param minCapacity
     */
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

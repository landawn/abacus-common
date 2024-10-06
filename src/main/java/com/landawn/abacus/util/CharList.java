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
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.CharStream;

/**
 *
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class CharList extends PrimitiveList<Character, char[], CharList> {

    private static final long serialVersionUID = 7293826835233022514L;

    static final Random RAND = new SecureRandom();

    private char[] elementData = N.EMPTY_CHAR_ARRAY;

    private int size = 0;

    /**
     *
     */
    public CharList() {
    }

    /**
     *
     *
     * @param initialCapacity
     */
    public CharList(final int initialCapacity) {
        elementData = initialCapacity == 0 ? N.EMPTY_CHAR_ARRAY : new char[initialCapacity];
    }

    /**
     * The specified array is used as the element array for this list without copying action.
     *
     * @param a
     */
    public CharList(final char[] a) {
        this(a, a.length);
    }

    /**
     *
     *
     * @param a
     * @param size
     * @throws IndexOutOfBoundsException
     */
    public CharList(final char[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, a.length);

        elementData = a;
        this.size = size;
    }

    /**
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static CharList of(final char... a) {
        return new CharList(N.nullToEmpty(a));
    }

    /**
     *
     *
     * @param a
     * @param size
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static CharList of(final char[] a, final int size) throws IndexOutOfBoundsException {
        N.checkFromIndexSize(0, size, N.len(a));

        return new CharList(N.nullToEmpty(a), size);
    }

    /**
     *
     * @param a
     * @return
     */
    public static CharList copyOf(final char[] a) {
        return of(N.clone(a));
    }

    /**
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
     *
     * @param c
     * @return
     */
    public static CharList from(final Collection<Character> c) {
        if (N.isEmpty(c)) {
            return new CharList();
        }

        return from(c, (char) 0);
    }

    /**
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static CharList from(final Collection<Character> c, final char defaultForNull) {
        if (N.isEmpty(c)) {
            return new CharList();
        }

        final char[] a = new char[c.size()];
        int idx = 0;

        for (final Character e : c) {
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
    public static CharList from(final Collection<Character> c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isEmpty(c)) {
            return new CharList();
        }

        return from(c, fromIndex, toIndex, (char) 0);
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param defaultForNull
     * @return
     */
    public static CharList from(final Collection<Character> c, final int fromIndex, final int toIndex, final char defaultForNull) {
        return of(N.toCharArray(c, fromIndex, toIndex, defaultForNull));
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @return
     */
    public static CharList range(final char startInclusive, final char endExclusive) {
        return of(Array.range(startInclusive, endExclusive));
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @param by
     * @return
     */
    public static CharList range(final char startInclusive, final char endExclusive, final int by) {
        return of(Array.range(startInclusive, endExclusive, by));
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @return
     */
    public static CharList rangeClosed(final char startInclusive, final char endInclusive) {
        return of(Array.rangeClosed(startInclusive, endInclusive));
    }

    /**
     *
     * @param startInclusive
     * @param endInclusive
     * @param by
     * @return
     */
    public static CharList rangeClosed(final char startInclusive, final char endInclusive, final int by) {
        return of(Array.rangeClosed(startInclusive, endInclusive, by));
    }

    /**
     *
     * @param element
     * @param len
     * @return
     */
    public static CharList repeat(final char element, final int len) {
        return of(Array.repeat(element, len));
    }

    /**
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
     *
     * @param candicates
     * @param len
     * @return
     */
    public static CharList random(final char[] candicates, final int len) {
        if (N.isEmpty(candicates) || candicates.length >= Integer.MAX_VALUE) {
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
    @Beta
    @Override
    public char[] array() {
        return elementData;
    }

    /**
     *
     * @param index
     * @return
     */
    public char get(final int index) {
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
    public char set(final int index, final char e) {
        rangeCheck(index);

        final char oldValue = elementData[index];

        elementData[index] = e;

        return oldValue;
    }

    /**
     *
     * @param e
     */
    public void add(final char e) {
        ensureCapacity(size + 1);

        elementData[size++] = e;
    }

    /**
     *
     * @param index
     * @param e
     */
    public void add(final int index, final char e) {
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
    public boolean addAll(final CharList c) {
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
    public boolean addAll(final int index, final CharList c) {
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
    public boolean addAll(final char[] a) {
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
    public boolean addAll(final int index, final char[] a) {
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
    public boolean remove(final char e) {
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
    public boolean removeAllOccurrences(final char e) {
        int w = 0;

        for (int i = 0; i < size; i++) {
            if (elementData[i] != e) {
                elementData[w++] = elementData[i];
            }
        }

        final int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, (char) 0);

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
    public boolean removeAll(final CharList c) {
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
    public boolean removeAll(final char[] a) {
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
    public <E extends Exception> boolean removeIf(final Throwables.CharPredicate<E> p) throws E {
        final CharList tmp = new CharList(size());

        for (int i = 0; i < size; i++) {
            if (!p.test(elementData[i])) {
                tmp.add(elementData[i]);
            }
        }

        if (tmp.size() == size()) {
            return false;
        }

        N.copy(tmp.elementData, 0, elementData, 0, tmp.size());
        N.fill(elementData, tmp.size(), size, (char) 0);
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
            final Set<Character> set = N.newLinkedHashSet(size);
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
            N.fill(elementData, idx + 1, size, (char) 0);

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
    public boolean retainAll(final CharList c) {
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
    public boolean retainAll(final char[] a) {
        if (N.isEmpty(a)) {
            final boolean result = size() > 0;
            clear();
            return result;
        }

        return retainAll(CharList.of(a));
    }

    /**
     *
     * @param c
     * @param complement
     * @return
     */
    private int batchRemove(final CharList c, final boolean complement) {
        final char[] elementData = this.elementData;//NOSONAR

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

        final int numRemoved = size - w;

        if (numRemoved > 0) {
            N.fill(elementData, w, size, (char) 0);

            size = w;
        }

        return numRemoved;
    }

    /**
     *
     * @param index
     * @return
     */
    public char delete(final int index) {
        rangeCheck(index);

        final char oldValue = elementData[index];

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

        final char[] tmp = N.deleteAllByIndices(elementData, indices);
        N.copy(tmp, 0, elementData, 0, tmp.length);
        N.fill(elementData, tmp.length, size, (char) 0);
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

        N.fill(elementData, newSize, size, (char) 0);

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
    public void replaceRange(final int fromIndex, final int toIndex, final CharList replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, (char) 0);
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
    public void replaceRange(final int fromIndex, final int toIndex, final char[] replacement) throws IndexOutOfBoundsException {
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
            N.fill(elementData, newSize, size, (char) 0);
        }

        this.size = newSize;
    }

    /**
     *
     * @param oldVal
     * @param newVal
     * @return
     */
    public int replaceAll(final char oldVal, final char newVal) {
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
    public <E extends Exception> void replaceAll(final Throwables.CharUnaryOperator<E> operator) throws E {
        for (int i = 0, len = size(); i < len; i++) {
            elementData[i] = operator.applyAsChar(elementData[i]);
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
    public <E extends Exception> boolean replaceIf(final Throwables.CharPredicate<E> predicate, final char newValue) throws E {
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
    public void fill(final char val) {
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
    public void fill(final int fromIndex, final int toIndex, final char val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        N.fill(elementData, fromIndex, toIndex, val);
    }

    /**
     *
     * @param valueToFind
     * @return
     */
    public boolean contains(final char valueToFind) {
        return indexOf(valueToFind) >= 0;
    }

    /**
     *
     * @param c
     * @return
     */
    @Override
    public boolean containsAny(final CharList c) {
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
    public boolean containsAny(final char[] a) {
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
    public boolean containsAll(final CharList c) {
        if (N.isEmpty(c)) {
            return true;
        } else if (isEmpty()) {
            return false;
        }

        final boolean isThisContainer = size() >= c.size();
        final CharList container = isThisContainer ? this : c;
        final char[] iterElements = isThisContainer ? c.array() : array();

        if (needToSet(size(), c.size())) {
            final Set<Character> set = container.toSet();

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
    public boolean containsAll(final char[] a) {
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
    public boolean disjoint(final CharList c) {
        if (isEmpty() || N.isEmpty(c)) {
            return true;
        }

        final boolean isThisContainer = size() >= c.size();
        final CharList container = isThisContainer ? this : c;
        final char[] iterElements = isThisContainer ? c.array() : array();

        if (needToSet(size(), c.size())) {
            final Set<Character> set = container.toSet();

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
    public boolean disjoint(final char[] b) {
        if (isEmpty() || N.isEmpty(b)) {
            return true;
        }

        return disjoint(of(b));
    }

    /**
     * Returns a new list with all the elements occurred in both <code>a</code> and <code>b</code>. Occurrences are considered.
     *
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    @Override
    public CharList intersection(final CharList b) {
        if (N.isEmpty(b)) {
            return new CharList();
        }

        final Multiset<Character> bOccurrences = b.toMultiset();

        final CharList c = new CharList(N.min(9, size(), b.size()));

        for (int i = 0, len = size(); i < len; i++) {
            if (bOccurrences.remove(elementData[i])) {
                c.add(elementData[i]);
            }
        }

        return c;
    }

    /**
     * Returns a new list with all the elements occurred in both <code>a</code> and <code>b</code>. Occurrences are considered.
     *
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    @Override
    public CharList intersection(final char[] b) {
        if (N.isEmpty(b)) {
            return new CharList();
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
    public CharList difference(final CharList b) {
        if (N.isEmpty(b)) {
            return of(N.copyOfRange(elementData, 0, size()));
        }

        final Multiset<Character> bOccurrences = b.toMultiset();

        final CharList c = new CharList(N.min(size(), N.max(9, size() - b.size())));

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
    public CharList difference(final char[] b) {
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
    public CharList symmetricDifference(final CharList b) {
        if (N.isEmpty(b)) {
            return this.copy();
        } else if (isEmpty()) {
            return b.copy();
        }

        final Multiset<Character> bOccurrences = b.toMultiset();
        final CharList c = new CharList(N.max(9, Math.abs(size() - b.size())));

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
    public CharList symmetricDifference(final char[] b) {
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
    public int occurrencesOf(final char valueToFind) {
        return N.occurrencesOf(elementData, valueToFind);
    }

    /**
     *
     * @param valueToFind
     * @return
     */
    public int indexOf(final char valueToFind) {
        return indexOf(valueToFind, 0);
    }

    /**
     *
     * @param valueToFind
     * @param fromIndex
     * @return
     */
    public int indexOf(final char valueToFind, final int fromIndex) {
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
    public int lastIndexOf(final char valueToFind) {
        return lastIndexOf(valueToFind, size);
    }

    /**
     * Last index of.
     * @param valueToFind
     * @param startIndexFromBack the start index to traverse backwards from. Inclusive.
     *
     * @return
     */
    public int lastIndexOf(final char valueToFind, final int startIndexFromBack) {
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
     *
     * @return
     */
    public OptionalChar min() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(N.min(elementData, 0, size));
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalChar min(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalChar.empty() : OptionalChar.of(N.min(elementData, fromIndex, toIndex));
    }

    /**
     *
     *
     * @return
     */
    public OptionalChar median() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(N.median(elementData, 0, size));
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalChar median(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalChar.empty() : OptionalChar.of(N.median(elementData, fromIndex, toIndex));
    }

    /**
     *
     *
     * @return
     */
    public OptionalChar max() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(N.max(elementData, 0, size));
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public OptionalChar max(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return fromIndex == toIndex ? OptionalChar.empty() : OptionalChar.of(N.max(elementData, fromIndex, toIndex));
    }

    /**
     *
     * @param k
     * @return
     */
    public OptionalChar kthLargest(final int k) {
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
    public OptionalChar kthLargest(final int fromIndex, final int toIndex, final int k) throws IllegalArgumentException, IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);
        N.checkArgPositive(k, cs.k);

        return toIndex - fromIndex < k ? OptionalChar.empty() : OptionalChar.of(N.kthLargest(elementData, fromIndex, toIndex, k));
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
    public <E extends Exception> void forEach(final Throwables.CharConsumer<E> action) throws E {
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
    public <E extends Exception> void forEach(final int fromIndex, final int toIndex, final Throwables.CharConsumer<E> action)
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
    public <E extends Exception> void forEachIndexed(final Throwables.IntCharConsumer<E> action) throws E {
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
    public <E extends Exception> void forEachIndexed(final int fromIndex, final int toIndex, final Throwables.IntCharConsumer<E> action)
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
    public OptionalChar first() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(elementData[0]);
    }

    /**
     *
     *
     * @return
     */
    public OptionalChar last() {
        return size() == 0 ? OptionalChar.empty() : OptionalChar.of(elementData[size() - 1]);
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
    public CharList distinct(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
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
    public int binarySearch(final char valueToFind) {
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
    public int binarySearch(final int fromIndex, final int toIndex, final char valueToFind) throws IndexOutOfBoundsException {
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

    /**
     *
     *
     * @return
     */
    @Override
    public CharList copy() {
        return new CharList(N.copyOfRange(elementData, 0, size));
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
    public CharList copy(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return new CharList(N.copyOfRange(elementData, fromIndex, toIndex));
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
    public CharList copy(final int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex);

        return new CharList(N.copyOfRange(elementData, fromIndex, toIndex, step));
    }

    /**
     * Returns List of {@code CharList} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     * @throws IndexOutOfBoundsException
     */
    @Override
    public List<CharList> split(final int fromIndex, final int toIndex, final int chunkSize) throws IndexOutOfBoundsException {
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
    public List<Character> boxed() {
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
    public List<Character> boxed(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final List<Character> res = new ArrayList<>(toIndex - fromIndex);

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
    public <C extends Collection<Character>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
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
    public Multiset<Character> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<Character>> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        final Multiset<Character> multiset = supplier.apply(toIndex - fromIndex);

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
    public CharIterator iterator() {
        if (isEmpty()) {
            return CharIterator.EMPTY;
        }

        return CharIterator.of(elementData, 0, size);
    }

    /**
     *
     *
     * @return
     */
    public CharStream stream() {
        return CharStream.of(elementData, 0, size());
    }

    /**
     *
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public CharStream stream(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex);

        return CharStream.of(elementData, fromIndex, toIndex);
    }

    /**
     * Returns the first element in the list.
     *
     * @return The first char value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public char getFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[0];
    }

    /**
     * Returns the last element in the list.
     *
     * @return The last char value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public char getLast() {
        throwNoSuchElementExceptionIfEmpty();

        return elementData[size - 1];
    }

    /**
     * Inserts the specified element at the beginning of this list.
     *
     * @param e the element to add
     */
    public void addFirst(final char e) {
        add(0, e);
    }

    /**
     * Inserts the specified element at the end of this list.
     *
     * @param e the element to add
     */
    public void addLast(final char e) {
        add(size, e);
    }

    /**
     * Removes and returns the first element from this list.
     *
     * @return The first char value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public char removeFirst() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(0);
    }

    /**
     * Removes and returns the last element from this list.
     *
     * @return The last char value in the list.
     * @throws NoSuchElementException if the list is empty.
     */
    public char removeLast() {
        throwNoSuchElementExceptionIfEmpty();

        return delete(size - 1);
    }

    /**
     * Returns a new CharList with the elements in reverse order.
     *
     * @return A new CharList with all elements of the current list in reverse order.
     */
    public CharList reversed() {
        final char[] a = N.copyOfRange(elementData, 0, size);

        N.reverse(a);

        return new CharList(a);
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
    public <R, E extends Exception> R apply(final Throwables.Function<? super CharList, ? extends R, E> func) throws E {
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
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super CharList, ? extends R, E> func) throws E {
        return isEmpty() ? Optional.<R> empty() : Optional.ofNullable(func.apply(this));
    }

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    @Override
    public <E extends Exception> void accept(final Throwables.Consumer<? super CharList, E> action) throws E {
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
    public <E extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super CharList, E> action) throws E {
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
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final CharList other) {
            return size == other.size && N.equals(elementData, 0, other.elementData, 0, size);
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
        return size == 0 ? Strings.STR_FOR_EMPTY_ARRAY : N.toString(elementData, 0, size);
    }

    private void ensureCapacity(final int minCapacity) {
        if (minCapacity > MAX_ARRAY_SIZE || minCapacity < 0) {
            throw new OutOfMemoryError();
        }

        if (N.isEmpty(elementData)) {
            elementData = new char[Math.max(DEFAULT_CAPACITY, minCapacity)];
        } else if (minCapacity - elementData.length > 0) {
            final int newCapacity = calNewCapacity(minCapacity, elementData.length);

            elementData = Arrays.copyOf(elementData, newCapacity);
        }
    }
}

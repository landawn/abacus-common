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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.IntFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;

/**
 *
 * @author Haiyang Li
 * @param <B>
 * @param <A>
 * @param <L>
 * @since 0.8
 */
public abstract class PrimitiveList<B, A, L extends PrimitiveList<B, A, L>> implements RandomAccess, java.io.Serializable {

    private static final long serialVersionUID = 1504784980113045443L;

    /**
     * Default initial capacity.
     */
    static final int DEFAULT_CAPACITY = 10;

    static final int MAX_ARRAY_SIZE = N.MAX_ARRAY_SIZE;

    /**
     * Returned the backed array.
     *
     * @return
     * @deprecated should call {@code toArray()}
     */
    @Deprecated
    @Beta
    public abstract A array();

    public abstract boolean addAll(L c);

    public abstract boolean addAll(int index, L c);

    public abstract boolean addAll(A a);

    public abstract boolean addAll(int index, A a);

    public abstract boolean removeAll(L c);

    public abstract boolean removeAll(A a);

    public abstract boolean removeDuplicates();

    public abstract boolean retainAll(L c);

    public abstract boolean retainAll(A a);

    public abstract void deleteAll(int... indices);

    public abstract void deleteRange(int fromIndex, int toIndex);

    public abstract void moveRange(int fromIndex, int toIndex, int newPositionStartIndex);

    public abstract void replaceRange(int fromIndex, int toIndex, A replacement);

    public abstract boolean containsAny(L l);

    public abstract boolean containsAny(A a);

    public abstract boolean containsAll(L l);

    public abstract boolean containsAll(A a);

    public abstract boolean disjoint(L l);

    public abstract boolean disjoint(A a);

    public abstract boolean hasDuplicates();

    public abstract L intersection(final L b);

    public abstract L intersection(final A a);

    public abstract L difference(final L b);

    public abstract L difference(final A a);

    public abstract L symmetricDifference(final L b);

    public abstract L symmetricDifference(final A a);

    /**
     *
     * @return a new List with distinct elements
     */
    public L distinct() {
        return distinct(0, size());
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return a new List with distinct elements
     */
    public abstract L distinct(final int fromIndex, final int toIndex);

    public abstract boolean isSorted();

    public abstract void sort();

    public abstract void reverseSort();

    public abstract void reverse();

    public abstract void reverse(final int fromIndex, final int toIndex);

    public abstract void rotate(int distance);

    public abstract void shuffle();

    public abstract void shuffle(final Random rnd);

    public abstract void swap(int i, int j);

    /**
     *
     * @return a copy of this List
     */
    public abstract L copy();

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public abstract L copy(final int fromIndex, final int toIndex);

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     */
    public abstract L copy(final int fromIndex, final int toIndex, final int step);

    /**
     * Returns consecutive sub lists of this list, each of the same size (the final list may be smaller),
     * or an empty List if the specified list is null or empty.
     *
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    public List<L> split(int chunkSize) {
        return split(0, size(), chunkSize);
    }

    /**
     * Returns List of {@code PrimitiveList 'L'} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     *
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    public abstract List<L> split(final int fromIndex, final int toIndex, int chunkSize);

    //    public List<L> split(P predicate) {
    //        return split(0, size(), predicate);
    //    }
    //
    //    /**
    //     * Split the List by the specified predicate.
    //     *
    //     * <pre>
    //     * <code>
    //     * // split the number sequence by window 5.
    //     * final MutableInt border = MutableInt.of(5);
    //     * IntList.of(1, 2, 3, 5, 7, 9, 10, 11, 19).split(e -> {
    //     *     if (e <= border.intValue()) {
    //     *         return true;
    //     *     } else {
    //     *         border.addAndGet(5);
    //     *         return false;
    //     *     }
    //     * }).forEach(N::println);
    //     * </code>
    //     * </pre>
    //     *
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param predicate
    //     * @return
    //     */
    //    public abstract List<L> split(final int fromIndex, final int toIndex, P predicate);

    public String join() {
        return join(N.ELEMENT_SEPARATOR);
    }

    /**
     *
     * @param delimiter
     * @return
     */
    public String join(final char delimiter) {
        return join(0, size(), delimiter);
    }

    /**
     *
     * @param delimiter
     * @return
     */
    public String join(final String delimiter) {
        return join(0, size(), delimiter);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public String join(final int fromIndex, final int toIndex) {
        return join(fromIndex, toIndex, N.ELEMENT_SEPARATOR);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public abstract String join(final int fromIndex, final int toIndex, final char delimiter);

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param delimiter
     * @return
     */
    public abstract String join(final int fromIndex, final int toIndex, final String delimiter);

    /**
     * Trim to size and return {@code this) list. There is no new list instance created.
     *
     * @return this List with trailing unused space removed.
     */
    @Beta
    public abstract L trimToSize();

    /**
     * Clear.
     */
    public abstract void clear();

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    public abstract boolean isEmpty();

    public abstract int size();

    public abstract A toArray();

    public List<B> toList() {
        return toList(0, size());
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public List<B> toList(final int fromIndex, final int toIndex) {
        return toCollection(fromIndex, toIndex, Factory.<B> ofList());
    }

    public Set<B> toSet() {
        return toSet(0, size());
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public Set<B> toSet(final int fromIndex, final int toIndex) {
        return toCollection(fromIndex, toIndex, Factory.<B> ofSet());
    }

    /**
     *
     * @param <C>
     * @param supplier
     * @return
     */
    public <C extends Collection<B>> C toCollection(final IntFunction<? extends C> supplier) {
        return toCollection(0, size(), supplier);
    }

    /**
     *
     * @param <C>
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    public abstract <C extends Collection<B>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier);

    public Multiset<B> toMultiset() {
        return toMultiset(0, size());
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public Multiset<B> toMultiset(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        final IntFunction<Multiset<B>> supplier = createMultisetSupplier();

        return toMultiset(fromIndex, toIndex, supplier);
    }

    /**
     *
     * @param supplier
     * @return
     */
    public Multiset<B> toMultiset(final IntFunction<Multiset<B>> supplier) {
        return toMultiset(0, size(), supplier);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    public abstract Multiset<B> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<B>> supplier);

    /**
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    public abstract <R, E extends Exception> R apply(Throwables.Function<? super L, R, E> func) throws E;

    /**
     * Apply if not empty.
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E the e
     */
    public abstract <R, E extends Exception> Optional<R> applyIfNotEmpty(Throwables.Function<? super L, R, E> func) throws E;

    /**
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public abstract <E extends Exception> void accept(Throwables.Consumer<? super L, E> action) throws E;

    /**
     * Accept if not empty.
     *
     * @param <E>
     * @param action
     * @throws E the e
     */
    public abstract <E extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super L, E> action) throws E;

    public void println() {
        N.println(toString());
    }

    protected void checkFromToIndex(final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, size());
    }

    protected int calNewCapacity(final int minCapacity, final int curLen) {
        int newCapacity = (int) (curLen * 1.75);

        if (newCapacity < 0 || newCapacity > MAX_ARRAY_SIZE) {
            newCapacity = MAX_ARRAY_SIZE;
        }

        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }
        return newCapacity;
    }

    protected <T> IntFunction<List<T>> createListSupplier() {
        return Factory.ofList();
    }

    protected <T> IntFunction<Set<T>> createSetSupplier() {
        return Factory.ofSet();
    }

    protected <K, V> IntFunction<Map<K, V>> createMapSupplier() {
        return Factory.ofMap();
    }

    protected <T> IntFunction<Multiset<T>> createMultisetSupplier() {
        return Factory.ofMultiset();
    }

    protected boolean needToSet(int lenA, int lenB) {
        return Math.min(lenA, lenB) > 3 && Math.max(lenA, lenB) > 9;
    }
}

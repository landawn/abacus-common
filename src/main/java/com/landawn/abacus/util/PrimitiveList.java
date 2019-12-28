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

import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntFunction;

// TODO: Auto-generated Javadoc
/**
 * The Class PrimitiveList.
 *
 * @author Haiyang Li
 * @param <B>
 * @param <A>
 * @param <L>
 * @since 0.8
 */
public abstract class PrimitiveList<B, A, L extends PrimitiveList<B, A, L>> implements RandomAccess, java.io.Serializable {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1504784980113045443L;

    /**
     * Default initial capacity.
     */
    static final int DEFAULT_CAPACITY = 10;

    /**
     * The maximum size of array to allocate. Some VMs reserve some header words in an array. Attempts to allocate
     * larger arrays may result in OutOfMemoryError: Requested array size exceeds VM limit
     */
    static final int MAX_ARRAY_SIZE = N.MAX_ARRAY_SIZE;

    /**
     *
     * @param minCapacity
     * @return
     */
    static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) {
            throw new OutOfMemoryError();
        }

        return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
    }

    /**
     * Returned the backed array.
     *
     * @return
     */
    public abstract A array();

    /**
     * Adds the all.
     *
     * @param a
     * @return true, if successful
     */
    public abstract boolean addAll(A a);

    /**
     * Adds the all.
     *
     * @param index
     * @param a
     * @return true, if successful
     */
    public abstract boolean addAll(int index, A a);

    /**
     * Removes the all.
     *
     * @param a
     * @return true, if successful
     */
    public abstract boolean removeAll(A a);

    /**
     *
     * @param indices
     */
    public abstract void deleteAll(int... indices);

    /**
     *
     * @param fromIndex
     * @param toIndex
     */
    public abstract void deleteRange(int fromIndex, int toIndex);

    // public abstract boolean containsAll(L l);

    /**
     *
     * @param a
     * @return true, if successful
     */
    public abstract boolean containsAll(A a);

    // public abstract boolean containsAny(L l);

    /**
     *
     * @param a
     * @return true, if successful
     */
    public abstract boolean containsAny(A a);

    // public abstract boolean disjoint(L l);

    /**
     *
     * @param a
     * @return true, if successful
     */
    public abstract boolean disjoint(A a);

    /**
     * Checks for duplicates.
     *
     * @return true, if successful
     */
    public abstract boolean hasDuplicates();

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
     * @return
     */
    public abstract L distinct(final int fromIndex, final int toIndex);

    /**
     * Sort.
     */
    public abstract void sort();

    /**
     * Reverse.
     */
    public abstract void reverse();

    /**
     *
     * @param fromIndex
     * @param toIndex
     */
    public abstract void reverse(final int fromIndex, final int toIndex);

    /**
     *
     * @param distance
     */
    public abstract void rotate(int distance);

    /**
     * Shuffle.
     */
    public abstract void shuffle();

    /**
     *
     * @param rnd
     */
    public abstract void shuffle(final Random rnd);

    /**
     *
     * @param i
     * @param j
     */
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
     * @param from
     * @param to
     * @param step
     * @return
     */
    public abstract L copy(final int from, final int to, final int step);

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

    /**
     *
     * @return
     */
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
     * Trim to size.
     *
     * @return this List with trailing unused space removed.
     */
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

    /**
     *
     * @return
     */
    public abstract int size();

    /**
     *
     * @return
     */
    public abstract A toArray();

    /**
     *
     * @return
     */
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

    /**
     *
     * @return
     */
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

    /**
     *
     * @return
     */
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

    /**
     * Println.
     */
    public void println() {
        N.println(toString());
    }

    /**
     * Check from to index.
     *
     * @param fromIndex
     * @param toIndex
     */
    protected void checkFromToIndex(final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, size());
    }

    /**
     * Creates the list supplier.
     *
     * @param <T>
     * @return
     */
    protected <T> IntFunction<List<T>> createListSupplier() {
        return Factory.ofList();
    }

    /**
     * Creates the set supplier.
     *
     * @param <T>
     * @return
     */
    protected <T> IntFunction<Set<T>> createSetSupplier() {
        return Factory.ofSet();
    }

    /**
     * Creates the map supplier.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    protected <K, V> IntFunction<Map<K, V>> createMapSupplier() {
        return Factory.ofMap();
    }

    /**
     * Creates the multiset supplier.
     *
     * @param <T>
     * @return
     */
    protected <T> IntFunction<Multiset<T>> createMultisetSupplier() {
        return Factory.ofMultiset();
    }

    /**
     * Need to set.
     *
     * @param lenA
     * @param lenB
     * @return true, if successful
     */
    protected boolean needToSet(int lenA, int lenB) {
        return Math.min(lenA, lenB) > 3 && Math.max(lenA, lenB) > 9;
    }
}

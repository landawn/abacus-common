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
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.IntFunction;

/**
 *
 * @since 0.8
 *
 * @author Haiyang Li
 */
public abstract class PrimitiveList<B, A, L extends PrimitiveList<B, A, L>> implements RandomAccess, java.io.Serializable {
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
     * 
     * @param a
     */
    public abstract boolean addAll(A a);

    /**
     * 
     * @param index
     * @param a
     */
    public abstract boolean addAll(int index, A a);

    /**
     * 
     * @param a
     */
    public abstract boolean removeAll(A a);

    public abstract void deleteAll(int... indices);

    public abstract void deleteRange(int fromIndex, int toIndex);

    // public abstract boolean containsAll(L l);

    public abstract boolean containsAll(A a);

    // public abstract boolean containsAny(L l);

    public abstract boolean containsAny(A a);

    // public abstract boolean disjoint(L l);

    public abstract boolean disjoint(A a);

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
     *
     */
    public abstract void sort();

    /**
     * 
     */
    public abstract void reverse();

    /**
     * 
     */
    public abstract void reverse(final int fromIndex, final int toIndex);

    /**
     * 
     * @param distance
     */
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
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
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

    public String join(final char delimiter) {
        return join(0, size(), delimiter);
    }

    public String join(final String delimiter) {
        return join(0, size(), delimiter);
    }

    public String join(final int fromIndex, final int toIndex) {
        return join(fromIndex, toIndex, N.ELEMENT_SEPARATOR);
    }

    public abstract String join(final int fromIndex, final int toIndex, final char delimiter);

    public abstract String join(final int fromIndex, final int toIndex, final String delimiter);

    /**
     * 
     *
     * @return this List with trailing unused space removed.
     */
    public abstract L trimToSize();

    public abstract void clear();

    public abstract boolean isEmpty();

    public abstract int size();

    public abstract A toArray();

    public List<B> toList() {
        return toList(0, size());
    }

    public List<B> toList(final int fromIndex, final int toIndex) {
        return toCollection(fromIndex, toIndex, Factory.<B> ofList());
    }

    public Set<B> toSet() {
        return toSet(0, size());
    }

    public Set<B> toSet(final int fromIndex, final int toIndex) {
        return toCollection(fromIndex, toIndex, Factory.<B> ofSet());
    }

    public <C extends Collection<B>> C toCollection(final IntFunction<? extends C> supplier) {
        return toCollection(0, size(), supplier);
    }

    public abstract <C extends Collection<B>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier);

    public Multiset<B> toMultiset() {
        return toMultiset(0, size());
    }

    public Multiset<B> toMultiset(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        final IntFunction<Multiset<B>> supplier = createMultisetSupplier();

        return toMultiset(fromIndex, toIndex, supplier);
    }

    public Multiset<B> toMultiset(final IntFunction<Multiset<B>> supplier) {
        return toMultiset(0, size(), supplier);
    }

    public abstract Multiset<B> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<B>> supplier);

    public abstract <R, E extends Exception> R apply(Try.Function<? super L, R, E> func) throws E;

    public abstract <R, E extends Exception> Optional<R> applyIfNotEmpty(Try.Function<? super L, R, E> func) throws E;

    public abstract <E extends Exception> void accept(Try.Consumer<? super L, E> action) throws E;

    public abstract <E extends Exception> void acceptIfNotEmpty(Try.Consumer<? super L, E> action) throws E;

    public void println() {
        N.println(toString());
    }

    protected void checkFromToIndex(final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, size());
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

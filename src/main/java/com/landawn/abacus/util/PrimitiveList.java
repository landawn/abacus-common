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

// TODO: Auto-generated Javadoc
/**
 * The Class PrimitiveList.
 *
 * @author Haiyang Li
 * @param <B> the generic type
 * @param <A> the generic type
 * @param <L> the generic type
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
     * Huge capacity.
     *
     * @param minCapacity the min capacity
     * @return the int
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
     * @return the a
     */
    public abstract A array();

    /**
     * Adds the all.
     *
     * @param a the a
     * @return true, if successful
     */
    public abstract boolean addAll(A a);

    /**
     * Adds the all.
     *
     * @param index the index
     * @param a the a
     * @return true, if successful
     */
    public abstract boolean addAll(int index, A a);

    /**
     * Removes the all.
     *
     * @param a the a
     * @return true, if successful
     */
    public abstract boolean removeAll(A a);

    /**
     * Delete all.
     *
     * @param indices the indices
     */
    public abstract void deleteAll(int... indices);

    /**
     * Delete range.
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     */
    public abstract void deleteRange(int fromIndex, int toIndex);

    // public abstract boolean containsAll(L l);

    /**
     * Contains all.
     *
     * @param a the a
     * @return true, if successful
     */
    public abstract boolean containsAll(A a);

    // public abstract boolean containsAny(L l);

    /**
     * Contains any.
     *
     * @param a the a
     * @return true, if successful
     */
    public abstract boolean containsAny(A a);

    // public abstract boolean disjoint(L l);

    /**
     * Disjoint.
     *
     * @param a the a
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
     * Distinct.
     *
     * @return a new List with distinct elements
     */
    public L distinct() {
        return distinct(0, size());
    }

    /**
     * Distinct.
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     * @return the l
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
     * Reverse.
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     */
    public abstract void reverse(final int fromIndex, final int toIndex);

    /**
     * Rotate.
     *
     * @param distance the distance
     */
    public abstract void rotate(int distance);

    /**
     * Shuffle.
     */
    public abstract void shuffle();

    /**
     * Shuffle.
     *
     * @param rnd the rnd
     */
    public abstract void shuffle(final Random rnd);

    /**
     * Swap.
     *
     * @param i the i
     * @param j the j
     */
    public abstract void swap(int i, int j);

    /**
     * Copy.
     *
     * @return a copy of this List
     */
    public abstract L copy();

    /**
     * Copy.
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     * @return the l
     */
    public abstract L copy(final int fromIndex, final int toIndex);

    /**
     * Copy.
     *
     * @param from the from
     * @param to the to
     * @param step the step
     * @return the l
     */
    public abstract L copy(final int from, final int to, final int step);

    /**
     * Returns consecutive sub lists of this list, each of the same size (the final list may be smaller),
     * or an empty List if the specified list is null or empty.
     *
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return the list
     */
    public List<L> split(int chunkSize) {
        return split(0, size(), chunkSize);
    }

    /**
     * Returns List of {@code PrimitiveList 'L'} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *  
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return the list
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
     * Join.
     *
     * @return the string
     */
    public String join() {
        return join(N.ELEMENT_SEPARATOR);
    }

    /**
     * Join.
     *
     * @param delimiter the delimiter
     * @return the string
     */
    public String join(final char delimiter) {
        return join(0, size(), delimiter);
    }

    /**
     * Join.
     *
     * @param delimiter the delimiter
     * @return the string
     */
    public String join(final String delimiter) {
        return join(0, size(), delimiter);
    }

    /**
     * Join.
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     * @return the string
     */
    public String join(final int fromIndex, final int toIndex) {
        return join(fromIndex, toIndex, N.ELEMENT_SEPARATOR);
    }

    /**
     * Join.
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     * @param delimiter the delimiter
     * @return the string
     */
    public abstract String join(final int fromIndex, final int toIndex, final char delimiter);

    /**
     * Join.
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     * @param delimiter the delimiter
     * @return the string
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
     * Size.
     *
     * @return the int
     */
    public abstract int size();

    /**
     * To array.
     *
     * @return the a
     */
    public abstract A toArray();

    /**
     * To list.
     *
     * @return the list
     */
    public List<B> toList() {
        return toList(0, size());
    }

    /**
     * To list.
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     * @return the list
     */
    public List<B> toList(final int fromIndex, final int toIndex) {
        return toCollection(fromIndex, toIndex, Factory.<B> ofList());
    }

    /**
     * To set.
     *
     * @return the sets the
     */
    public Set<B> toSet() {
        return toSet(0, size());
    }

    /**
     * To set.
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     * @return the sets the
     */
    public Set<B> toSet(final int fromIndex, final int toIndex) {
        return toCollection(fromIndex, toIndex, Factory.<B> ofSet());
    }

    /**
     * To collection.
     *
     * @param <C> the generic type
     * @param supplier the supplier
     * @return the c
     */
    public <C extends Collection<B>> C toCollection(final IntFunction<? extends C> supplier) {
        return toCollection(0, size(), supplier);
    }

    /**
     * To collection.
     *
     * @param <C> the generic type
     * @param fromIndex the from index
     * @param toIndex the to index
     * @param supplier the supplier
     * @return the c
     */
    public abstract <C extends Collection<B>> C toCollection(final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier);

    /**
     * To multiset.
     *
     * @return the multiset
     */
    public Multiset<B> toMultiset() {
        return toMultiset(0, size());
    }

    /**
     * To multiset.
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     * @return the multiset
     */
    public Multiset<B> toMultiset(final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex);

        final IntFunction<Multiset<B>> supplier = createMultisetSupplier();

        return toMultiset(fromIndex, toIndex, supplier);
    }

    /**
     * To multiset.
     *
     * @param supplier the supplier
     * @return the multiset
     */
    public Multiset<B> toMultiset(final IntFunction<Multiset<B>> supplier) {
        return toMultiset(0, size(), supplier);
    }

    /**
     * To multiset.
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     * @param supplier the supplier
     * @return the multiset
     */
    public abstract Multiset<B> toMultiset(final int fromIndex, final int toIndex, final IntFunction<Multiset<B>> supplier);

    /**
     * Apply.
     *
     * @param <R> the generic type
     * @param <E> the element type
     * @param func the func
     * @return the r
     * @throws E the e
     */
    public abstract <R, E extends Exception> R apply(Try.Function<? super L, R, E> func) throws E;

    /**
     * Apply if not empty.
     *
     * @param <R> the generic type
     * @param <E> the element type
     * @param func the func
     * @return the optional
     * @throws E the e
     */
    public abstract <R, E extends Exception> Optional<R> applyIfNotEmpty(Try.Function<? super L, R, E> func) throws E;

    /**
     * Accept.
     *
     * @param <E> the element type
     * @param action the action
     * @throws E the e
     */
    public abstract <E extends Exception> void accept(Try.Consumer<? super L, E> action) throws E;

    /**
     * Accept if not empty.
     *
     * @param <E> the element type
     * @param action the action
     * @throws E the e
     */
    public abstract <E extends Exception> void acceptIfNotEmpty(Try.Consumer<? super L, E> action) throws E;

    /**
     * Println.
     */
    public void println() {
        N.println(toString());
    }

    /**
     * Check from to index.
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     */
    protected void checkFromToIndex(final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, size());
    }

    /**
     * Creates the list supplier.
     *
     * @param <T> the generic type
     * @return the int function
     */
    protected <T> IntFunction<List<T>> createListSupplier() {
        return Factory.ofList();
    }

    /**
     * Creates the set supplier.
     *
     * @param <T> the generic type
     * @return the int function
     */
    protected <T> IntFunction<Set<T>> createSetSupplier() {
        return Factory.ofSet();
    }

    /**
     * Creates the map supplier.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the int function
     */
    protected <K, V> IntFunction<Map<K, V>> createMapSupplier() {
        return Factory.ofMap();
    }

    /**
     * Creates the multiset supplier.
     *
     * @param <T> the generic type
     * @return the int function
     */
    protected <T> IntFunction<Multiset<T>> createMultisetSupplier() {
        return Factory.ofMultiset();
    }

    /**
     * Need to set.
     *
     * @param lenA the len A
     * @param lenB the len B
     * @return true, if successful
     */
    protected boolean needToSet(int lenA, int lenB) {
        return Math.min(lenA, lenB) > 3 && Math.max(lenA, lenB) > 9;
    }
}

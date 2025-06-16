/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.io.Serial;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.IntFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.Fn.Factory;

/**
 * The PrimitiveList is an abstract class that represents a list of primitive data types.
 * It provides a blueprint for classes that need to implement a list of primitives.
 * This class implements the RandomAccess and Serializable interfaces.
 *
 * @param <B> the boxed type of the primitive, for example, Integer for int, Double for double, etc.
 * @param <A> the array type of the primitive, for example, int[] for int, double[] for double, etc.
 * @param <L> the type of the list itself, used for methods that return the list. It must extend PrimitiveList.
 *
 */
public abstract class PrimitiveList<B, A, L extends PrimitiveList<B, A, L>> implements RandomAccess, java.io.Serializable { // Iterable<B>, // reference to notEmpty is ambiguous both methods notEmpty(java.lang.Iterable<?>)

    @Serial
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

    /**
     *
     * @param c
     * @return
     */
    public abstract boolean addAll(L c);

    /**
     *
     * @param index
     * @param c
     * @return
     */
    public abstract boolean addAll(int index, L c);

    /**
     *
     * @param a
     * @return
     */
    public abstract boolean addAll(A a);

    /**
     *
     * @param index
     * @param a
     * @return
     */
    public abstract boolean addAll(int index, A a);

    /**
     *
     * @param c
     * @return
     */
    public abstract boolean removeAll(L c);

    /**
     *
     * @param a
     * @return
     */
    public abstract boolean removeAll(A a);

    public abstract boolean removeDuplicates();

    /**
     *
     * @param c
     * @return
     */
    public abstract boolean retainAll(L c);

    /**
     *
     * @param a
     * @return
     */
    public abstract boolean retainAll(A a);

    /**
     *
     * @param indices
     */
    public abstract void deleteAllByIndices(int... indices);

    /**
     *
     * @param fromIndex
     * @param toIndex
     */
    public abstract void deleteRange(int fromIndex, int toIndex);

    /**
     * Moves a range of elements in this list to a new position within the list.
     * The new position specified by {@code newPositionStartIndexAfterMove} is the start index of the specified range after the move operation, not before the move operation.
     * <br />
     * No elements are deleted in the process, this list maintains its size.
     *
     * @param fromIndex the starting index (inclusive) of the range to be moved
     * @param toIndex the ending index (exclusive) of the range to be moved
     * @param newPositionStartIndexAfterMove the start index of the specified range after the move operation, not before the move operation. 
     *          It must in the range: [0, array.length - (toIndex - fromIndex)]
     * @throws IndexOutOfBoundsException if the range is out of the list bounds or newPositionStartIndexAfterMove is invalid
     */
    public abstract void moveRange(int fromIndex, int toIndex, int newPositionStartIndexAfterMove);

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param replacement
     */
    public abstract void replaceRange(int fromIndex, int toIndex, L replacement);

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param replacement
     */
    public abstract void replaceRange(int fromIndex, int toIndex, A replacement);

    /**
     *
     * @param l
     * @return
     */
    public abstract boolean containsAny(L l);

    /**
     *
     * @param a
     * @return
     */
    public abstract boolean containsAny(A a);

    /**
     *
     * @param l
     * @return
     */
    public abstract boolean containsAll(L l);

    /**
     *
     * @param a
     * @return
     */
    public abstract boolean containsAll(A a);

    /**
     *
     * @param l
     * @return
     */
    public abstract boolean disjoint(L l);

    /**
     *
     * @param a
     * @return
     */
    public abstract boolean disjoint(A a);

    /**
     * Returns a new list containing elements that are present in both this list and the specified list.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences present in both lists.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * IntList list2 = IntList.of(1, 2, 2, 4);
     * IntList result = list1.intersection(list2); // result will be [1, 2, 2]
     * // One occurrence of '1' (minimum count in both lists) and two occurrences of '2'
     *
     * IntList list3 = IntList.of(5, 5, 6);
     * IntList list4 = IntList.of(5, 7);
     * IntList result2 = list3.intersection(list4); // result will be [5]
     * // One occurrence of '5' (minimum count in both lists)
     * </pre>
     *
     * @param b the list to find common elements with this list
     * @return a new IntList containing elements present in both this list and the specified list,
     *         considering the minimum number of occurrences in either list.
     *         Returns an empty list if either list is {@code null} or empty.
     * @see IntList#intersection(IntList)
     * @see N#intersection(int[], int[])
     */
    public abstract L intersection(final L b);

    /**
     * Returns a new list containing elements that are present in both this list and the specified array.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences present in both sources.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * int[] array = new int[]{1, 2, 2, 4};
     * IntList result = list1.intersection(array); // result will be [1, 2, 2]
     * // One occurrence of '1' (minimum count in both sources) and two occurrences of '2'
     *
     * IntList list2 = IntList.of(5, 5, 6);
     * int[] array2 = new int[]{5, 7};
     * IntList result2 = list2.intersection(array2); // result will be [5]
     * // One occurrence of '5' (minimum count in both sources)
     * </pre>
     *
     * @param b the array to find common elements with this list
     * @return a new IntList containing elements present in both this list and the specified array,
     *         considering the minimum number of occurrences in either source.
     *         Returns an empty list if the array is {@code null} or empty.
     * @see IntList#intersection(int[])
     * @see N#intersection(int[], int[])
     */
    public abstract L intersection(final A b);

    /**
     * Returns a new list with the elements in this list but not in the specified list {@code b},
     * considering the number of occurrences of each element.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * IntList list2 = IntList.of(2, 5, 1);
     * IntList result = list1.difference(list2); // result will be [0, 2, 3]
     * // One '2' remains because list1 has two occurrences and list2 has one
     *
     * IntList list3 = IntList.of(5, 6);
     * IntList list4 = IntList.of(5, 5, 6);
     * IntList result2 = list3.difference(list4); // result will be [] (empty)
     * // No elements remain because list4 has at least as many occurrences of each value as list3
     * </pre>
     *
     * @param b the list to compare against this list
     * @return a new IntList containing the elements that are present in this list but not in the specified list,
     *         considering the number of occurrences.
     * @see IntList#difference(IntList)
     * @see N#difference(int[], int[])
     */
    public abstract L difference(final L b);

    /**
     * Returns a new list with the elements in this list but not in the specified array {@code b},
     * considering the number of occurrences of each element.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(0, 1, 2, 2, 3);
     * int[] array = new int[]{2, 5, 1};
     * IntList result = list1.difference(array); // result will be [0, 2, 3]
     * // One '2' remains because list1 has two occurrences and array has one
     *
     * IntList list2 = IntList.of(5, 6);
     * int[] array2 = new int[]{5, 5, 6};
     * IntList result2 = list2.difference(array2); // result will be [] (empty)
     * // No elements remain because array2 has at least as many occurrences of each value as list2
     * </pre>
     *
     * @param a the array to compare against this list
     * @return a new IntList containing the elements that are present in this list but not in the specified array,
     *         considering the number of occurrences.
     *         Returns a copy of this list if {@code b} is {@code null} or empty.
     * @see IntList#difference(int[])
     * @see N#difference(int[], int[])
     */
    public abstract L difference(final A a);

    /**
     * Returns a new list containing elements that are present in either this list or the specified list,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(1, 1, 2, 3);
     * IntList list2 = IntList.of(1, 2, 2, 4);
     * IntList result = list1.symmetricDifference(list2);
     * // result will contain: [1, 3, 2, 4]
     * // Elements explanation:
     * // - 1 appears twice in list1 and once in list2, so one occurrence remains
     * // - 3 appears only in list1, so it remains
     * // - 2 appears once in list1 and twice in list2, so one occurrence remains
     * // - 4 appears only in list2, so it remains
     * </pre>
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified list that aren't in this list.
     *
     * @param b the list to compare with this list for symmetric difference
     * @return a new list containing elements that are present in either this list or the specified list,
     *         but not in both, considering the number of occurrences
     * @see IntList#symmetricDifference(IntList)
     * @see N#symmetricDifference(int[], int[])
     */
    public abstract L symmetricDifference(final L b);

    /**
     * Returns a new list containing elements that are present in either this list or the specified array,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>Example:
     * <pre>
     * IntList list1 = IntList.of(1, 1, 2, 3);
     * int[] array = new int[]{1, 2, 2, 4};
     * IntList result = list1.symmetricDifference(array);
     * // result will contain: [1, 3, 2, 4]
     * // Elements explanation:
     * // - 1 appears twice in list1 and once in array, so one occurrence remains
     * // - 3 appears only in list1, so it remains
     * // - 2 appears once in list1 and twice in array, so one occurrence remains
     * // - 4 appears only in array, so it remains
     * </pre>
     *
     * <p>The order of elements is preserved, with elements from this list appearing first,
     * followed by elements from the specified array.
     *
     * @param b the array to compare with this list for symmetric difference
     * @return a new list containing elements that are present in either this list or the specified array,
     *         but not in both, considering the number of occurrences
     * @see IntList#symmetricDifference(int[])
     * @see N#symmetricDifference(int[], int[])
     */
    public abstract L symmetricDifference(final A b);

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
     * @return a new List with distinct elements
     */
    public abstract L distinct(final int fromIndex, final int toIndex);

    public abstract boolean isSorted();

    public abstract void sort();

    public abstract void reverseSort();

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
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     */
    public abstract L copy(final int fromIndex, final int toIndex, final int step);

    /**
     * Returns consecutive sub lists of this list, each of the same size (the final list may be smaller),
     * or an empty List if the specified list is {@code null} or empty.
     *
     * @param chunkSize the desired size of each subsequence (the last may be smaller).
     * @return
     */
    public List<L> split(final int chunkSize) {
        return split(0, size(), chunkSize);
    }

    /**
     * Returns List of {@code PrimitiveList 'L'} with consecutive subsequences of the elements, each of the same size (the final sequence may be smaller).
     *
     *
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each subsequence (the last may be smaller).
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
     * Trim to size and return {@code this} list. There is no new list instance created.
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
     * @return {@code true}, if is empty
     */
    public abstract boolean isEmpty();

    public abstract int size();

    public abstract A toArray();

    public List<B> boxed() {
        return boxed(0, size());
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public abstract List<B> boxed(final int fromIndex, final int toIndex);

    /**
     *
     * @return
     * @deprecated use {@link #boxed()} instead.
     */
    @Deprecated
    public List<B> toList() {
        return boxed();
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     * @deprecated use {@link #boxed(int, int)} instead.
     */
    @Deprecated
    public List<B> toList(final int fromIndex, final int toIndex) {
        return boxed(fromIndex, toIndex);
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
        return toCollection(fromIndex, toIndex, Factory.ofSet());
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
     * @throws IndexOutOfBoundsException
     */
    public Multiset<B> toMultiset(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
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

    public abstract Iterator<B> iterator();

    //    /**
    //     *
    //     * @param <R>
    //     * @param <E>
    //     * @param func
    //     * @return
    //     * @throws E the e
    //     */
    //    public abstract <R, E extends Exception> R apply(Throwables.Function<? super L, ? extends R, E> func) throws E;
    //
    //    /**
    //     * Apply if not empty.
    //     *
    //     * @param <R>
    //     * @param <E>
    //     * @param func
    //     * @return
    //     * @throws E the e
    //     */
    //    public abstract <R, E extends Exception> Optional<R> applyIfNotEmpty(Throwables.Function<? super L, ? extends R, E> func) throws E;
    //
    //    /**
    //     *
    //     * @param <E>
    //     * @param action
    //     * @throws E the e
    //     */
    //    public abstract <E extends Exception> void accept(Throwables.Consumer<? super L, E> action) throws E;
    //
    //    /**
    //     * Accept if not empty.
    //     *
    //     * @param <E>
    //     * @param action
    //     * @return
    //     * @throws E the e
    //     */
    //    public abstract <E extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super L, E> action) throws E;

    @Beta
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

    protected boolean needToSet(final int lenA, final int lenB) {
        return Math.min(lenA, lenB) > 3 && Math.max(lenA, lenB) > 9;
    }

    protected void throwNoSuchElementExceptionIfEmpty() {
        if (size() == 0) {
            throw new NoSuchElementException(this.getClass().getSimpleName() + " is empty");
        }
    }
}

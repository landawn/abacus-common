/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * An immutable, thread-safe implementation of the SortedSet interface.
 * Once created, the contents of an ImmutableSortedSet cannot be modified.
 * All mutating operations inherited from the parent interfaces will throw UnsupportedOperationException.
 * 
 * <p>This class maintains elements in sorted order according to their natural ordering
 * (if they implement Comparable) or by a Comparator provided at set creation time.
 * The implementation preserves the sorted order of elements when created from another SortedSet.</p>
 * 
 * <p>This class provides several static factory methods for creating instances:
 * <ul>
 * <li>{@link #empty()} - returns an empty immutable sorted set</li>
 * <li>{@link #of(...)} - creates sets with specific elements</li>
 * <li>{@link #copyOf(Collection)} - creates a defensive copy from another collection</li>
 * <li>{@link #wrap(SortedSet)} - wraps an existing sorted set (changes to the underlying set will be reflected)</li>
 * </ul>
 * </p>
 * 
 * <p>Example usage:
 * <pre>{@code
 * ImmutableSortedSet<String> set = ImmutableSortedSet.of("apple", "banana", "cherry");
 * ImmutableSortedSet<String> subset = set.subSet("banana", "cherry");
 * }</pre>
 * </p>
 *
 * @param <E> the type of elements maintained by this set
 * @see ImmutableSet
 * @see SortedSet
 */
@SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
@SuppressWarnings("java:S2160")
public class ImmutableSortedSet<E> extends ImmutableSet<E> implements SortedSet<E> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableSortedSet EMPTY = new ImmutableSortedSet(N.emptySortedSet());

    private final SortedSet<E> sortedSet;

    ImmutableSortedSet(final SortedSet<? extends E> sortedSet) {
        super(sortedSet);
        this.sortedSet = (SortedSet<E>) sortedSet;
    }

    /**
     * Returns an empty ImmutableSortedSet. This method always returns the same cached instance,
     * making it memory efficient for representing empty sorted sets.
     *
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedSet<String> emptySet = ImmutableSortedSet.empty();
     * System.out.println(emptySet.size()); // 0
     * }</pre>
     * </p>
     *
     * @param <E> the type of elements in the set
     * @return an empty ImmutableSortedSet instance
     */
    public static <E> ImmutableSortedSet<E> empty() {
        return EMPTY;
    }

    /**
     * Returns an ImmutableSortedSet containing a single element.
     * The element must implement Comparable to determine its natural ordering.
     *
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedSet<Integer> singletonSet = ImmutableSortedSet.just(42);
     * System.out.println(singletonSet.first()); // 42
     * }</pre>
     * </p>
     *
     * @param <E> the type of element, must extend Comparable
     * @param e the element to be contained in the set
     * @return an ImmutableSortedSet containing only the specified element
     * @throws ClassCastException if the element does not implement Comparable
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> just(final E e) {
        return new ImmutableSortedSet<>(new TreeSet<>(Collections.singletonList(e)));
    }

    /**
     * Returns an ImmutableSortedSet containing a single element.
     * This method is equivalent to {@link #just(Comparable)} and exists for API consistency.
     *
     * @param <E> the type of element, must extend Comparable
     * @param e1 the element to be contained in the set
     * @return an ImmutableSortedSet containing only the specified element
     * @see #just(Comparable)
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1) {
        return new ImmutableSortedSet<>(new TreeSet<>(Collections.singletonList(e1)));
    }

    /**
     * Returns an ImmutableSortedSet containing exactly two elements in sorted order.
     * The elements must implement Comparable to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedSet<String> set = ImmutableSortedSet.of("beta", "alpha");
     * System.out.println(set); // [alpha, beta]
     * }</pre>
     * </p>
     *
     * @param <E> the type of elements, must extend Comparable
     * @param e1 the first element
     * @param e2 the second element
     * @return an ImmutableSortedSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2)));
    }

    /**
     * Returns an ImmutableSortedSet containing exactly three elements in sorted order.
     * The elements must implement Comparable to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * @param <E> the type of elements, must extend Comparable
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @return an ImmutableSortedSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3)));
    }

    /**
     * Returns an ImmutableSortedSet containing exactly four elements in sorted order.
     * The elements must implement Comparable to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * @param <E> the type of elements, must extend Comparable
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @return an ImmutableSortedSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4)));
    }

    /**
     * Returns an ImmutableSortedSet containing exactly five elements in sorted order.
     * The elements must implement Comparable to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * @param <E> the type of elements, must extend Comparable
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @return an ImmutableSortedSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5)));
    }

    /**
     * Returns an ImmutableSortedSet containing exactly six elements in sorted order.
     * The elements must implement Comparable to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * @param <E> the type of elements, must extend Comparable
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @return an ImmutableSortedSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6)));
    }

    /**
     * Returns an ImmutableSortedSet containing exactly seven elements in sorted order.
     * The elements must implement Comparable to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * @param <E> the type of elements, must extend Comparable
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @return an ImmutableSortedSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6,
            final E e7) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7)));
    }

    /**
     * Returns an ImmutableSortedSet containing exactly eight elements in sorted order.
     * The elements must implement Comparable to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * @param <E> the type of elements, must extend Comparable
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @param e8 the eighth element
     * @return an ImmutableSortedSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7,
            final E e8) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8)));
    }

    /**
     * Returns an ImmutableSortedSet containing exactly nine elements in sorted order.
     * The elements must implement Comparable to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * @param <E> the type of elements, must extend Comparable
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @param e8 the eighth element
     * @param e9 the ninth element
     * @return an ImmutableSortedSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7,
            final E e8, final E e9) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9)));
    }

    /**
     * Returns an ImmutableSortedSet containing exactly ten elements in sorted order.
     * The elements must implement Comparable to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * @param <E> the type of elements, must extend Comparable
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @param e8 the eighth element
     * @param e9 the ninth element
     * @param e10 the tenth element
     * @return an ImmutableSortedSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7,
            final E e8, final E e9, final E e10) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10)));
    }

    /**
     * Returns an ImmutableSortedSet containing the elements of the specified collection.
     * If the provided collection is already an instance of ImmutableSortedSet, it is directly returned.
     * If the provided collection is {@code null} or empty, an empty ImmutableSortedSet is returned.
     * Otherwise, a new ImmutableSortedSet is created with the elements of the provided collection.
     * 
     * <p>The elements are sorted according to their natural ordering if they implement Comparable,
     * or a ClassCastException will be thrown if they don't.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * List<String> list = Arrays.asList("charlie", "alpha", "beta");
     * ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(list);
     * System.out.println(set); // [alpha, beta, charlie]
     * }</pre>
     * </p>
     *
     * @param <E> the type of elements in the collection
     * @param c the collection whose elements are to be placed into this set
     * @return an ImmutableSortedSet containing the elements of the specified collection
     * @throws ClassCastException if the elements don't implement Comparable
     */
    public static <E> ImmutableSortedSet<E> copyOf(final Collection<? extends E> c) {
        if (c instanceof ImmutableSortedSet) {
            return (ImmutableSortedSet<E>) c;
        } else if (N.isEmpty(c)) {
            return empty();
        } else if (c instanceof SortedSet sortedSet) {
            return new ImmutableNavigableSet<>(new TreeSet<>(sortedSet));
        } else {
            return new ImmutableSortedSet<>(new TreeSet<>(c));
        }
    }

    /**
     * Wraps the provided SortedSet into an ImmutableSortedSet. Changes to the specified SortedSet 
     * will be reflected in the ImmutableSortedSet.
     * If the provided SortedSet is already an instance of ImmutableSortedSet, it is directly returned.
     * If the provided SortedSet is {@code null}, an empty ImmutableSortedSet is returned.
     * Otherwise, returns a new ImmutableSortedSet backed by the provided SortedSet.
     * 
     * <p><b>Warning:</b> This method does not create a defensive copy. Changes to the underlying
     * SortedSet will be visible through the returned ImmutableSortedSet, which violates the
     * immutability contract. Use {@link #copyOf(Collection)} for a true immutable copy.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * SortedSet<String> mutableSet = new TreeSet<>();
     * ImmutableSortedSet<String> immutableView = ImmutableSortedSet.wrap(mutableSet);
     * mutableSet.add("new element"); // This change is visible in immutableView!
     * }</pre>
     * </p>
     *
     * @param <E> the type of elements in the set
     * @param sortedSet the sorted set to wrap
     * @return an {@code ImmutableSortedSet} backed by the specified {@code sortedSet}
     */
    @Beta
    public static <E> ImmutableSortedSet<E> wrap(final SortedSet<? extends E> sortedSet) {
        if (sortedSet instanceof ImmutableSortedSet) {
            return (ImmutableSortedSet<E>) sortedSet;
        } else if (sortedSet == null) {
            return empty();
        } else {
            return new ImmutableSortedSet<>(sortedSet);
        }
    }

    /**
     * This method is deprecated and will throw an UnsupportedOperationException if used.
     * Use {@link #wrap(SortedSet)} for SortedSet or {@link ImmutableSet#wrap(Set)} for regular Sets.
     *
     * @param <E> the type of elements
     * @param set the set parameter (ignored)
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    public static <E> ImmutableSet<E> wrap(final Set<? extends E> set) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the comparator used to order the elements in this set, or {@code null} if
     * this set uses the natural ordering of its elements.
     *
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedSet<String> naturalOrder = ImmutableSortedSet.of("a", "b", "c");
     * System.out.println(naturalOrder.comparator()); // null
     * 
     * Comparator<String> reverseOrder = Comparator.reverseOrder();
     * SortedSet<String> customSet = new TreeSet<>(reverseOrder);
     * customSet.add("a");
     * ImmutableSortedSet<String> customOrder = ImmutableSortedSet.wrap(customSet);
     * System.out.println(customOrder.comparator()); // ReverseComparator
     * }</pre>
     * </p>
     *
     * @return the comparator used to order the elements in this set, or {@code null}
     *         if natural ordering is used
     */
    @Override
    public Comparator<? super E> comparator() {
        return sortedSet.comparator();
    }

    /**
     * Returns a view of the portion of this set whose elements range from {@code fromElement},
     * inclusive, to {@code toElement}, exclusive. The returned set is backed by this set,
     * so it remains immutable.
     * 
     * <p>The returned set will throw an {@code IllegalArgumentException} on an attempt to insert
     * an element outside its range.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3, 4, 5);
     * ImmutableSortedSet<Integer> subset = set.subSet(2, 4);
     * System.out.println(subset); // [2, 3]
     * }</pre>
     * </p>
     *
     * @param fromElement low endpoint (inclusive) of the returned set
     * @param toElement high endpoint (exclusive) of the returned set
     * @return a view of the portion of this set whose elements range from
     *         {@code fromElement}, inclusive, to {@code toElement}, exclusive
     * @throws ClassCastException if {@code fromElement} and {@code toElement}
     *         cannot be compared to one another using this set's comparator
     *         is null and this set uses natural ordering, or its comparator
     *         does not permit null elements
     * @throws IllegalArgumentException if {@code fromElement} is greater than
     *         {@code toElement}; or if this set itself has a restricted range,
     *         and {@code fromElement} or {@code toElement} lies outside the
     *         bounds of the range
     */
    @Override
    public ImmutableSortedSet<E> subSet(final E fromElement, final E toElement) {
        return wrap(sortedSet.subSet(fromElement, toElement));
    }

    /**
     * Returns a view of the portion of this set whose elements are strictly less than
     * {@code toElement}. The returned set is backed by this set, so it remains immutable.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedSet<String> set = ImmutableSortedSet.of("a", "b", "c", "d");
     * ImmutableSortedSet<String> headSet = set.headSet("c");
     * System.out.println(headSet); // [a, b]
     * }</pre>
     * </p>
     *
     * @param toElement high endpoint (exclusive) of the returned set
     * @return a view of the portion of this set whose elements are strictly
     *         less than {@code toElement}
     * @throws ClassCastException if {@code toElement} is not compatible with
     *         this set's comparator
     *         natural ordering, or its comparator does not permit null elements
     * @throws IllegalArgumentException if this set itself has a restricted range,
     *         and {@code toElement} lies outside the bounds of the range
     */
    @Override
    public ImmutableSortedSet<E> headSet(final E toElement) {
        return wrap(sortedSet.headSet(toElement));
    }

    /**
     * Returns a view of the portion of this set whose elements are greater than or equal to
     * {@code fromElement}. The returned set is backed by this set, so it remains immutable.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(10, 20, 30, 40);
     * ImmutableSortedSet<Integer> tailSet = set.tailSet(25);
     * System.out.println(tailSet); // [30, 40]
     * }</pre>
     * </p>
     *
     * @param fromElement low endpoint (inclusive) of the returned set
     * @return a view of the portion of this set whose elements are greater
     *         than or equal to {@code fromElement}
     * @throws ClassCastException if {@code fromElement} is not compatible with
     *         this set's comparator
     *         natural ordering, or its comparator does not permit null elements
     * @throws IllegalArgumentException if this set itself has a restricted range,
     *         and {@code fromElement} lies outside the bounds of the range
     */
    @Override
    public ImmutableSortedSet<E> tailSet(final E fromElement) {
        return wrap(sortedSet.tailSet(fromElement));
    }

    /**
     * Returns the first (lowest) element currently in this set.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedSet<String> set = ImmutableSortedSet.of("banana", "apple", "cherry");
     * System.out.println(set.first()); // "apple"
     * }</pre>
     * </p>
     *
     * @return the first (lowest) element currently in this set
     * @throws NoSuchElementException if this set is empty
     */
    @Override
    public E first() {
        return sortedSet.first();
    }

    /**
     * Returns the last (highest) element currently in this set.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(3, 1, 4, 1, 5);
     * System.out.println(set.last()); // 5
     * }</pre>
     * </p>
     *
     * @return the last (highest) element currently in this set
     * @throws NoSuchElementException if this set is empty
     */
    @Override
    public E last() {
        return sortedSet.last();
    }
}
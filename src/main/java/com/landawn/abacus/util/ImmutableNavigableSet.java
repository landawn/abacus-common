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
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * An immutable, thread-safe implementation of the NavigableSet interface.
 * This class extends ImmutableSortedSet and provides additional navigation methods
 * for accessing elements based on their ordering.
 * 
 * <p>A NavigableSet extends SortedSet with navigation methods returning the closest
 * matches for given search targets. Methods like {@link #lower}, {@link #floor},
 * {@link #ceiling}, and {@link #higher} return elements respectively less than,
 * less than or equal, greater than or equal, and greater than a given element,
 * returning null if there is no such element.</p>
 * 
 * <p>All mutating operations will throw UnsupportedOperationException. The set maintains
 * elements in sorted order according to their natural ordering or by a Comparator provided
 * at creation time.</p>
 * 
 * <p>Example usage:
 * <pre>{@code
 * ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);
 * 
 * System.out.println(set.floor(6));        // 5
 * System.out.println(set.higher(5));       // 7
 * System.out.println(set.descendingSet()); // [9, 7, 5, 3, 1]
 * }</pre>
 * </p>
 *
 * @param <E> the type of elements maintained by this set
 * @see ImmutableSortedSet
 * @see NavigableSet
 */
@SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
public final class ImmutableNavigableSet<E> extends ImmutableSortedSet<E> implements NavigableSet<E> { //NOSONAR

    @SuppressWarnings("rawtypes")
    private static final ImmutableNavigableSet EMPTY = new ImmutableNavigableSet(N.emptyNavigableSet());

    private final NavigableSet<E> navigableSet;

    ImmutableNavigableSet(final NavigableSet<? extends E> navigableSet) {
        super(navigableSet);
        this.navigableSet = (NavigableSet<E>) navigableSet;
    }

    /**
     * Returns an empty ImmutableNavigableSet. This method always returns the same cached instance,
     * making it memory efficient for representing empty navigable sets.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableSet<String> emptySet = ImmutableNavigableSet.empty();
     * System.out.println(emptySet.size()); // 0
     * }</pre>
     * </p>
     *
     * @param <E> the type of elements in the set
     * @return an empty ImmutableNavigableSet instance
     */
    public static <E> ImmutableNavigableSet<E> empty() {
        return EMPTY;
    }

    /**
     * Returns an ImmutableNavigableSet containing a single element.
     * The element must implement Comparable to determine its natural ordering.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableSet<Integer> singletonSet = ImmutableNavigableSet.just(42);
     * System.out.println(singletonSet.first()); // 42
     * }</pre>
     * </p>
     *
     * @param <E> the type of element, must extend Comparable
     * @param e the element to be contained in the set
     * @return an ImmutableNavigableSet containing only the specified element
     * @throws ClassCastException if the element does not implement Comparable
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> just(final E e) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Collections.singletonList(e)));
    }

    /**
     * Returns an ImmutableNavigableSet containing a single element.
     * This method is equivalent to {@link #just(Comparable)} and exists for API consistency.
     *
     * @param <E> the type of element, must extend Comparable
     * @param e1 the element to be contained in the set
     * @return an ImmutableNavigableSet containing only the specified element
     * @see #just(Comparable)
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Collections.singletonList(e1)));
    }

    /**
     * Returns an ImmutableNavigableSet containing exactly two elements in sorted order.
     * The elements must implement Comparable to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableSet<String> set = ImmutableNavigableSet.of("beta", "alpha");
     * System.out.println(set); // [alpha, beta]
     * }</pre>
     * </p>
     *
     * @param <E> the type of elements, must extend Comparable
     * @param e1 the first element
     * @param e2 the second element
     * @return an ImmutableNavigableSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2)));
    }

    /**
     * Returns an ImmutableNavigableSet containing exactly three elements in sorted order.
     * The elements must implement Comparable to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * @param <E> the type of elements, must extend Comparable
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @return an ImmutableNavigableSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3)));
    }

    /**
     * Returns an ImmutableNavigableSet containing exactly four elements in sorted order.
     * The elements must implement Comparable to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * @param <E> the type of elements, must extend Comparable
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @return an ImmutableNavigableSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4)));
    }

    /**
     * Returns an ImmutableNavigableSet containing exactly five elements in sorted order.
     * The elements must implement Comparable to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * @param <E> the type of elements, must extend Comparable
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @return an ImmutableNavigableSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5)));
    }

    /**
     * Returns an ImmutableNavigableSet containing exactly six elements in sorted order.
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
     * @return an ImmutableNavigableSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6)));
    }

    /**
     * Returns an ImmutableNavigableSet containing exactly seven elements in sorted order.
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
     * @return an ImmutableNavigableSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6,
            final E e7) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7)));
    }

    /**
     * Returns an ImmutableNavigableSet containing exactly eight elements in sorted order.
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
     * @return an ImmutableNavigableSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6,
            final E e7, final E e8) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8)));
    }

    /**
     * Returns an ImmutableNavigableSet containing exactly nine elements in sorted order.
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
     * @return an ImmutableNavigableSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6,
            final E e7, final E e8, final E e9) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9)));
    }

    /**
     * Returns an ImmutableNavigableSet containing exactly ten elements in sorted order.
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
     * @return an ImmutableNavigableSet containing the specified elements in sorted order
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6,
            final E e7, final E e8, final E e9, final E e10) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10)));
    }

    /**
     * Returns an ImmutableNavigableSet containing the elements of the specified collection.
     * If the provided collection is already an instance of ImmutableNavigableSet, it is directly returned.
     * If the provided collection is {@code null} or empty, an empty ImmutableNavigableSet is returned.
     * Otherwise, a new ImmutableNavigableSet is created with the elements of the provided collection.
     * 
     * <p>The elements are sorted according to their natural ordering if they implement Comparable,
     * or a ClassCastException will be thrown if they don't.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * List<String> list = Arrays.asList("charlie", "alpha", "beta");
     * ImmutableNavigableSet<String> set = ImmutableNavigableSet.copyOf(list);
     * System.out.println(set); // [alpha, beta, charlie]
     * }</pre>
     * </p>
     *
     * @param <E> the type of elements in the collection
     * @param c the collection whose elements are to be placed into this set
     * @return an ImmutableNavigableSet containing the elements of the specified collection
     * @throws ClassCastException if the elements don't implement Comparable
     */
    public static <E> ImmutableNavigableSet<E> copyOf(final Collection<? extends E> c) {
        if (c instanceof ImmutableNavigableSet) {
            return (ImmutableNavigableSet<E>) c;
        } else if (N.isEmpty(c)) {
            return empty();
        } else if (c instanceof SortedSet sortedSet) {
            return new ImmutableNavigableSet<>(new TreeSet<>(sortedSet));
        } else {
            return new ImmutableNavigableSet<>(new TreeSet<>(c));
        }
    }

    /**
     * Wraps the provided NavigableSet into an ImmutableNavigableSet. Changes to the specified 
     * NavigableSet will be reflected in the ImmutableNavigableSet.
     * If the provided NavigableSet is already an instance of ImmutableNavigableSet, it is directly returned.
     * If the provided NavigableSet is {@code null}, an empty ImmutableNavigableSet is returned.
     * Otherwise, returns a new ImmutableNavigableSet backed by provided NavigableSet.
     * 
     * <p><b>Warning:</b> This method does not create a defensive copy. Changes to the underlying
     * NavigableSet will be visible through the returned ImmutableNavigableSet, which violates the
     * immutability contract. Use {@link #copyOf(Collection)} for a true immutable copy.</p>
     * 
     * <p>Example:
     * <pre>{@code
     * NavigableSet<String> mutableSet = new TreeSet<>();
     * ImmutableNavigableSet<String> immutableView = ImmutableNavigableSet.wrap(mutableSet);
     * mutableSet.add("new element"); // This change is visible in immutableView!
     * }</pre>
     * </p>
     *
     * @param <E> the type of elements in the NavigableSet
     * @param navigableSet the NavigableSet to be wrapped into an ImmutableNavigableSet
     * @return an ImmutableNavigableSet backed by the provided NavigableSet
     */
    @Beta
    public static <E> ImmutableNavigableSet<E> wrap(final NavigableSet<? extends E> navigableSet) {
        if (navigableSet instanceof ImmutableNavigableSet) {
            return (ImmutableNavigableSet<E>) navigableSet;
        } else if (navigableSet == null) {
            return empty();
        } else {
            return new ImmutableNavigableSet<>(navigableSet);
        }
    }

    /**
     * This method is deprecated and will throw an UnsupportedOperationException if used.
     * Use {@link #wrap(NavigableSet)} for NavigableSet or {@link ImmutableSortedSet#wrap(SortedSet)} 
     * for regular SortedSets.
     *
     * @param <E> the type of elements
     * @param sortedSet ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    public static <E> ImmutableSortedSet<E> wrap(final SortedSet<? extends E> sortedSet) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the greatest element in this set strictly less than the given element,
     * or {@code null} if there is no such element.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);
     * System.out.println(set.lower(5));  // 3
     * System.out.println(set.lower(6));  // 5
     * System.out.println(set.lower(1));  // null
     * }</pre>
     * </p>
     *
     * @param e the value to match
     * @return the greatest element less than {@code e}, or {@code null} if there is no such element
     * @throws ClassCastException if the specified element cannot be compared with the elements currently in the set
     *         or its comparator does not permit null elements
     */
    @Override
    public E lower(final E e) {
        return navigableSet.lower(e);
    }

    /**
     * Returns the greatest element in this set less than or equal to the given element,
     * or {@code null} if there is no such element.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);
     * System.out.println(set.floor(5));  // 5
     * System.out.println(set.floor(6));  // 5
     * System.out.println(set.floor(0));  // null
     * }</pre>
     * </p>
     *
     * @param e the value to match
     * @return the greatest element less than or equal to {@code e}, or {@code null} if there is no such element
     * @throws ClassCastException if the specified element cannot be compared with the elements currently in the set
     *         or its comparator does not permit null elements
     */
    @Override
    public E floor(final E e) {
        return navigableSet.floor(e);
    }

    /**
     * Returns the least element in this set greater than or equal to the given element,
     * or {@code null} if there is no such element.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);
     * System.out.println(set.ceiling(5));  // 5
     * System.out.println(set.ceiling(6));  // 7
     * System.out.println(set.ceiling(10)); // null
     * }</pre>
     * </p>
     *
     * @param e the value to match
     * @return the least element greater than or equal to {@code e}, or {@code null} if there is no such element
     * @throws ClassCastException if the specified element cannot be compared with the elements currently in the set
     *         or its comparator does not permit null elements
     */
    @Override
    public E ceiling(final E e) {
        return navigableSet.ceiling(e);
    }

    /**
     * Returns the least element in this set strictly greater than the given element,
     * or {@code null} if there is no such element.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);
     * System.out.println(set.higher(5));  // 7
     * System.out.println(set.higher(6));  // 7
     * System.out.println(set.higher(9));  // null
     * }</pre>
     * </p>
     *
     * @param e the value to match
     * @return the least element greater than {@code e}, or {@code null} if there is no such element
     * @throws ClassCastException if the specified element cannot be compared with the elements currently in the set
     *         or its comparator does not permit null elements
     */
    @Override
    public E higher(final E e) {
        return navigableSet.higher(e);
    }

    /**
     * This operation is not supported by ImmutableNavigableSet.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableNavigableSet does not support modification operations
     */
    @Deprecated
    @Override
    public E pollFirst() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableNavigableSet.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated ImmutableNavigableSet does not support modification operations
     */
    @Deprecated
    @Override
    public E pollLast() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a reverse order view of the elements contained in this set.
     * The descending set is backed by this set, so it remains immutable.
     * The returned set has an ordering equivalent to Collections.reverseOrder(comparator()).
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 3, 5, 7, 9);
     * ImmutableNavigableSet<Integer> descending = set.descendingSet();
     * System.out.println(descending); // [9, 7, 5, 3, 1]
     * }</pre>
     * </p>
     *
     * @return a reverse order view of this set
     */
    @Override
    public ImmutableNavigableSet<E> descendingSet() {
        return wrap(navigableSet.descendingSet());
    }

    /**
     * Returns an iterator over the elements in this set, in descending order.
     * The iterator provides read-only access and does not support the remove() operation.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableSet<String> set = ImmutableNavigableSet.of("a", "b", "c");
     * ObjIterator<String> iter = set.descendingIterator();
     * while (iter.hasNext()) {
     *     System.out.print(iter.next() + " "); // c b a
     * }
     * }</pre>
     * </p>
     *
     * @return an iterator over the elements in this set, in descending order
     */
    @Override
    public ObjIterator<E> descendingIterator() {
        return ObjIterator.of(navigableSet.descendingIterator());
    }

    /**
     * Returns a view of the portion of this set whose elements range from {@code fromElement} to {@code toElement}.
     * If {@code fromInclusive} is {@code true}, the returned set includes {@code fromElement} if present.
     * If {@code toInclusive} is {@code true}, the returned set includes {@code toElement} if present.
     * The returned set is backed by this set, so it remains immutable.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
     * ImmutableNavigableSet<Integer> sub = set.subSet(2, true, 4, false);
     * System.out.println(sub); // [2, 3]
     * }</pre>
     * </p>
     *
     * @param fromElement low endpoint of the elements in the returned set
     * @param fromInclusive {@code true} if the low endpoint is to be included in the returned view
     * @param toElement high endpoint of the elements in the returned set
     * @param toInclusive {@code true} if the high endpoint is to be included in the returned view
     * @return a view of the portion of this set whose elements range from {@code fromElement} to {@code toElement}
     * @throws ClassCastException if {@code fromElement} and {@code toElement} cannot be compared to one another using this set's comparator
     * @throws IllegalArgumentException if {@code fromElement} is greater than {@code toElement}; or if this set itself has a restricted range, and {@code fromElement} or {@code toElement} lies outside the bounds of the range
     */
    @Override
    public ImmutableNavigableSet<E> subSet(final E fromElement, final boolean fromInclusive, final E toElement, final boolean toInclusive) {
        return wrap(navigableSet.subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    /**
     * Returns a view of the portion of this set whose elements are less than (or equal to, if {@code inclusive} is true) {@code toElement}.
     * The returned set is backed by this set, so it remains immutable.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
     * ImmutableNavigableSet<Integer> head = set.headSet(3, false);
     * System.out.println(head); // [1, 2]
     * }</pre>
     * </p>
     *
     * @param toElement high endpoint of the elements in the returned set
     * @param inclusive {@code true} if the high endpoint is to be included in the returned view
     * @return a view of the portion of this set whose elements are less than (or equal to, if {@code inclusive} is true) {@code toElement}
     * @throws ClassCastException if {@code toElement} is not compatible with this set's comparator
     * @throws IllegalArgumentException if this set itself has a restricted range, and {@code toElement} lies outside the bounds of the range
     */
    @Override
    public ImmutableNavigableSet<E> headSet(final E toElement, final boolean inclusive) {
        return wrap(navigableSet.headSet(toElement, inclusive));
    }

    /**
     * Returns a view of the portion of this set whose elements are greater than (or equal to, if {@code inclusive} is true) {@code fromElement}.
     * The returned set is backed by this set, so it remains immutable.
     * 
     * <p>Example:
     * <pre>{@code
     * ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4, 5);
     * ImmutableNavigableSet<Integer> tail = set.tailSet(3, false);
     * System.out.println(tail); // [4, 5]
     * }</pre>
     * </p>
     *
     * @param fromElement low endpoint of the elements in the returned set
     * @param inclusive {@code true} if the low endpoint is to be included in the returned view
     * @return a view of the portion of this set whose elements are greater than (or equal to, if {@code inclusive} is true) {@code fromElement}
     * @throws ClassCastException if {@code fromElement} is not compatible with this set's comparator
     * @throws IllegalArgumentException if this set itself has a restricted range, and {@code fromElement} lies outside the bounds of the range
     */
    @Override
    public ImmutableNavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
        return wrap(navigableSet.tailSet(fromElement, inclusive));
    }
}
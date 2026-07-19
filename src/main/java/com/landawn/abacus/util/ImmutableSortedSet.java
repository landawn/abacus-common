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
 * An immutable implementation of the {@link SortedSet} interface.
 * The contents of an {@code ImmutableSortedSet} cannot be modified through its API.
 * All mutating operations inherited from the parent interfaces throw {@link UnsupportedOperationException}.
 * An instance created by {@link #wrap(SortedSet)} reflects external changes to its backing set;
 * use {@link #copyOf(Collection)} when an independent immutable value is required.
 *
 * <p>This class maintains elements in sorted order according to their natural ordering
 * (if they implement {@link Comparable}) or by a {@link Comparator} provided at set creation time.
 * The implementation preserves the sorted order of elements when created from another {@code SortedSet}.</p>
 *
 * <p>This class provides several static factory methods for creating instances:</p>
 * <ul>
 * <li>{@link #empty()} - returns an empty immutable sorted set</li>
 * <li>{@link #of(Object)} (and arity-overloads up to ten elements) - creates sets with specific elements</li>
 * <li>{@link #copyOf(Collection)} - creates a defensive copy from another collection</li>
 * <li>{@link #wrap(SortedSet)} - wraps an existing sorted set (changes to the underlying set will be reflected)</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ImmutableSortedSet<String> set = ImmutableSortedSet.of("apple", "banana", "cherry");
 * ImmutableSortedSet<String> subset = set.subSet("banana", "cherry");
 * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<String> emptySet = ImmutableSortedSet.empty();
     * System.out.println(emptySet.size());   // prints 0
     * }</pre>
     *
     * @param <E> the type of elements in the set
     * @return an empty ImmutableSortedSet instance
     */
    public static <E> ImmutableSortedSet<E> empty() {
        return EMPTY;
    }

    /**
     * Returns an ImmutableSortedSet containing a single element in sorted order.
     * The element must implement {@link Comparable} to determine its natural ordering.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(42);
     * System.out.println(set.first());   // prints 42
     * }</pre>
     *
     * @param <E> the type of element, must extend {@link Comparable}
     * @param e1 the element to be contained in the set
     * @return an {@code ImmutableSortedSet} containing only the specified element
     * @throws NullPointerException if {@code e1} is {@code null}, since {@link java.util.TreeSet}
     *         does not allow {@code null} elements when using natural ordering
     * @see #of(Object, Object)
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1) {
        return new ImmutableSortedSet<>(new TreeSet<>(Collections.singletonList(e1)));
    }

    /**
     * Returns an ImmutableSortedSet containing up to two distinct elements in sorted order.
     * The elements must implement {@link Comparable} to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<String> set = ImmutableSortedSet.of("beta", "alpha");
     * System.out.println(set);   // prints [alpha, beta]
     * }</pre>
     *
     * @param <E> the type of elements, must extend {@link Comparable}
     * @param e1 the first element
     * @param e2 the second element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2)));
    }

    /**
     * Returns an ImmutableSortedSet containing up to three distinct elements in sorted order.
     * The elements must implement {@link Comparable} to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<String> set = ImmutableSortedSet.of("gamma", "alpha", "beta");
     * System.out.println(set);   // prints [alpha, beta, gamma]
     * }</pre>
     *
     * @param <E> the type of elements, must extend {@link Comparable}
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3)));
    }

    /**
     * Returns an ImmutableSortedSet containing up to four distinct elements in sorted order.
     * The elements must implement {@link Comparable} to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(3, 1, 4, 2);
     * System.out.println(set);   // prints [1, 2, 3, 4]
     * }</pre>
     *
     * @param <E> the type of elements, must extend {@link Comparable}
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4)));
    }

    /**
     * Returns an ImmutableSortedSet containing up to five distinct elements in sorted order.
     * The elements must implement {@link Comparable} to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<String> set = ImmutableSortedSet.of("Mon", "Tue", "Wed", "Thu", "Fri");
     * System.out.println(set);   // prints [Fri, Mon, Thu, Tue, Wed]
     * }</pre>
     *
     * @param <E> the type of elements, must extend {@link Comparable}
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5)));
    }

    /**
     * Returns an ImmutableSortedSet containing up to six distinct elements in sorted order.
     * The elements must implement {@link Comparable} to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(6, 1, 4, 2, 3, 5);
     * System.out.println(set);   // prints [1, 2, 3, 4, 5, 6]
     * }</pre>
     *
     * @param <E> the type of elements, must extend {@link Comparable}
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6)));
    }

    /**
     * Returns an ImmutableSortedSet containing up to seven distinct elements in sorted order.
     * The elements must implement {@link Comparable} to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<String> set = ImmutableSortedSet.of(
     *     "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
     * );
     * System.out.println(set.first());   // prints Fri
     * System.out.println(set.last());    // prints Wed
     * }</pre>
     *
     * @param <E> the type of elements, must extend {@link Comparable}
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6,
            final E e7) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7)));
    }

    /**
     * Returns an ImmutableSortedSet containing up to eight distinct elements in sorted order.
     * The elements must implement {@link Comparable} to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(8, 3, 5, 1, 2, 7, 4, 6);
     * System.out.println(set);   // prints [1, 2, 3, 4, 5, 6, 7, 8]
     * }</pre>
     *
     * @param <E> the type of elements, must extend {@link Comparable}
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @param e8 the eighth element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7,
            final E e8) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8)));
    }

    /**
     * Returns an ImmutableSortedSet containing up to nine distinct elements in sorted order.
     * The elements must implement {@link Comparable} to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
     * System.out.println(set.last());   // prints 9
     * }</pre>
     *
     * @param <E> the type of elements, must extend {@link Comparable}
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @param e8 the eighth element
     * @param e9 the ninth element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7,
            final E e8, final E e9) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9)));
    }

    /**
     * Returns an ImmutableSortedSet containing up to ten distinct elements in sorted order.
     * The elements must implement {@link Comparable} to determine their natural ordering.
     * Duplicate elements will be removed, potentially resulting in a set with fewer elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
     * System.out.println(set.size());   // prints 10
     * }</pre>
     *
     * @param <E> the type of elements, must extend {@link Comparable}
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
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7,
            final E e8, final E e9, final E e10) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10)));
    }

    /**
     * Returns an ImmutableSortedSet containing the elements of the specified array in natural order.
     * Duplicate elements are removed. If the array is {@code null} or empty, the cached empty set is returned.
     *
     * @param <E> the type of elements, which must be mutually comparable
     * @param a the array whose elements are to be copied
     * @return an ImmutableSortedSet containing the array elements
     * @throws NullPointerException if the array contains a {@code null} element
     */
    public static <E> ImmutableSortedSet<E> copyOf(final E[] a) {
        return N.isEmpty(a) ? empty() : new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(a)));
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> list = Arrays.asList("charlie", "alpha", "beta");
     * ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(list);
     * System.out.println(set);   // prints [alpha, beta, charlie]
     * }</pre>
     *
     * @param <E> the type of elements in the collection
     * @param c the collection whose elements are to be placed into this set
     * @return an {@code ImmutableSortedSet} containing the elements of the specified collection, or the same instance if it is already an
     *         {@code ImmutableSortedSet}. The comparator of a {@code SortedSet} source is retained even when the source is empty; a {@code null}
     *         or empty non-sorted source returns the shared empty instance.
     * @throws ClassCastException if the elements are not mutually comparable (when the source collection is not a {@code SortedSet})
     * @throws NullPointerException if the collection contains a {@code null} element and natural ordering is used
     * @see #wrap(SortedSet)
     */
    public static <E> ImmutableSortedSet<E> copyOf(final Collection<? extends E> c) {
        if (c instanceof ImmutableSortedSet) {
            return (ImmutableSortedSet<E>) c;
        } else if (c instanceof SortedSet sortedSet) {
            return new ImmutableSortedSet<>(new TreeSet<>(sortedSet));
        } else if (N.isEmpty(c)) {
            return empty();
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
     * immutability contract. Use {@link #copyOf(Collection)} for a {@code true} immutable copy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SortedSet<String> mutableSet = new TreeSet<>();
     * ImmutableSortedSet<String> immutableView = ImmutableSortedSet.wrap(mutableSet);
     * mutableSet.add("new element");   // this change is visible in immutableView!
     * }</pre>
     *
     * @param <E> the type of elements in the set
     * @param sortedSet the sorted set to wrap; may be {@code null}
     * @return an {@code ImmutableSortedSet} backed by the specified {@code sortedSet},
     *         the same instance if it is already an {@code ImmutableSortedSet},
     *         or {@link #empty()} if {@code sortedSet} is {@code null}
     * @see #copyOf(Collection)
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
     * This method is deprecated and always throws an {@link UnsupportedOperationException}.
     * Use {@link #wrap(SortedSet)} for a {@code SortedSet} or {@link ImmutableSet#wrap(Set)} for a regular {@code Set}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> set = new HashSet<>();
     * ImmutableSortedSet.wrap(set);   // throws UnsupportedOperationException
     * }</pre>
     *
     * @param <E> the type of elements
     * @param set the set parameter (ignored)
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated this overload is not supported and always throws {@code UnsupportedOperationException};
     *             use {@link #wrap(SortedSet)} for a {@code SortedSet} or {@link ImmutableSet#wrap(Set)} for a regular {@code Set}
     */
    @Deprecated
    public static <E> ImmutableSet<E> wrap(final Set<? extends E> set) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the comparator used to order the elements in this set, or {@code null} if
     * this set uses the natural ordering of its elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<String> naturalOrder = ImmutableSortedSet.of("a", "b", "c");
     * System.out.println(naturalOrder.comparator());   // prints null
     *
     * Comparator<String> reverseOrder = Comparator.reverseOrder();
     * SortedSet<String> customSet = new TreeSet<>(reverseOrder);
     * customSet.add("a");
     * ImmutableSortedSet<String> customOrder = ImmutableSortedSet.wrap(customSet);
     * System.out.println(customOrder.comparator() == reverseOrder);   // prints true
     * }</pre>
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
     * <p>The returned set is itself an {@code ImmutableSortedSet}; any attempt to modify it
     * throws an {@link UnsupportedOperationException}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3, 4, 5);
     * ImmutableSortedSet<Integer> subset = set.subSet(2, 4);
     * System.out.println(subset);   // prints [2, 3]
     * }</pre>
     *
     * @param fromElement low endpoint (inclusive) of the returned set
     * @param toElement high endpoint (exclusive) of the returned set
     * @return a view of the portion of this set whose elements range from
     *         {@code fromElement}, inclusive, to {@code toElement}, exclusive
     * @throws ClassCastException if {@code fromElement} and {@code toElement}
     *         cannot be compared to one another using this set's comparator
     *         (or, if the set has no comparator, using natural ordering)
     * @throws NullPointerException if {@code fromElement} or {@code toElement} is {@code null}
     *         and this set uses natural ordering, or its comparator does not permit {@code null} elements
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<String> set = ImmutableSortedSet.of("a", "b", "c", "d");
     * ImmutableSortedSet<String> headSet = set.headSet("c");
     * System.out.println(headSet);   // prints [a, b]
     * }</pre>
     *
     * @param toElement high endpoint (exclusive) of the returned set
     * @return a view of the portion of this set whose elements are strictly
     *         less than {@code toElement}
     * @throws ClassCastException if {@code toElement} is not compatible with
     *         this set's comparator (or, if the set has no comparator, using natural ordering)
     * @throws NullPointerException if {@code toElement} is {@code null} and this set uses natural ordering,
     *         or its comparator does not permit {@code null} elements
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(10, 20, 30, 40);
     * ImmutableSortedSet<Integer> tailSet = set.tailSet(25);
     * System.out.println(tailSet);   // prints [30, 40]
     * }</pre>
     *
     * @param fromElement low endpoint (inclusive) of the returned set
     * @return a view of the portion of this set whose elements are greater
     *         than or equal to {@code fromElement}
     * @throws ClassCastException if {@code fromElement} is not compatible with
     *         this set's comparator (or, if the set has no comparator, using natural ordering)
     * @throws NullPointerException if {@code fromElement} is {@code null} and this set uses natural ordering,
     *         or its comparator does not permit {@code null} elements
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<String> set = ImmutableSortedSet.of("banana", "apple", "cherry");
     * System.out.println(set.first());   // prints "apple"
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(3, 1, 4, 1, 5);
     * System.out.println(set.last());   // prints 5
     * }</pre>
     *
     * @return the last (highest) element currently in this set
     * @throws NoSuchElementException if this set is empty
     */
    @Override
    public E last() {
        return sortedSet.last();
    }
}

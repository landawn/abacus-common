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
 * <p>The natural-order {@code of(...)} factories require {@code E extends Comparable<? super E>}.
 * {@link #copyOf(Collection)} and {@link #wrap(SortedSet)} remain unbounded so a {@link Comparator} can
 * order elements that are not comparable. Because this class extends {@link ImmutableSet}, a call written
 * as {@code ImmutableSortedSet.of(nonComparableElement)} whose result is not demanded as an
 * {@code ImmutableSortedSet} can still resolve to {@link ImmutableSet#of(Object)} and return an unsorted
 * set.</p>
 *
 * @param <E> the type of elements maintained by this set
 * @see ImmutableSet
 * @see SortedSet
 */
@com.landawn.abacus.annotation.Immutable
@SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
@SuppressWarnings("java:S2160")
public class ImmutableSortedSet<E> extends ImmutableSet<E> implements SortedSet<E> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableSortedSet EMPTY = new ImmutableSortedSet(N.emptySortedSet(), true);

    private final SortedSet<E> sortedSet;

    /**
     * Constructs an ImmutableSortedSet backed by the provided sorted set.
     * The backing set is always exposed through an unmodifiable view; its concrete class name
     * is not treated as evidence that it is immutable.
     *
     * @param sortedSet the sorted set whose elements are to be included in this ImmutableSortedSet.
     * @throws NullPointerException if {@code sortedSet} is {@code null}
     */
    ImmutableSortedSet(final SortedSet<? extends E> sortedSet) throws NullPointerException {
        this(sortedSet, false);
    }

    /**
     * Constructs an {@code ImmutableSortedSet} backed by the provided sortedSet.
     *
     * @param sortedSet the sortedSet whose elements are to be included in this ImmutableSortedSet.
     * @param ownsBacking {@code true} only if no other modifiable reference to {@code sortedSet} survives
     *        this call; see {@link ImmutableCollection#ownsBacking}.
     * @throws NullPointerException if {@code sortedSet} is {@code null}
     */
    ImmutableSortedSet(final SortedSet<? extends E> sortedSet, final boolean ownsBacking) throws NullPointerException {
        // Always the 3-arg super(): this class's own 2-arg flag is ownsBacking, while ImmutableSet's used to
        // be isUnmodifiable, so a 2-arg super() call once bound to the wrong parameter - silently skipping the
        // unmodifiable wrapper AND dropping the flag. ImmutableSet no longer declares a 2-arg constructor, so
        // that mistake is now a compile error rather than a silent one; keep passing all three regardless.
        super(sortedSet, false, ownsBacking);

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
     * @param <E> the element type; must extend {@code Comparable<? super E>}
     * @param e1 the element to be contained in the set
     * @return an {@code ImmutableSortedSet} containing only the specified element
     * @throws NullPointerException if {@code e1} is {@code null}, since {@link java.util.TreeSet} does not allow {@code null} elements when using
     *         natural ordering
     * @throws ClassCastException if {@code e1} cannot be compared with itself in natural order
     * @see #of(Object, Object)
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1) throws NullPointerException, ClassCastException {
        return new ImmutableSortedSet<>(new TreeSet<>(Collections.singletonList(e1)), true);
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
     * @param <E> the element type; must extend {@code Comparable<? super E>}
     * @param e1 the first element
     * @param e2 the second element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     * @throws ClassCastException if the elements are not mutually comparable
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2) throws NullPointerException, ClassCastException {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2)), true);
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
     * @param <E> the element type; must extend {@code Comparable<? super E>}
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     * @throws ClassCastException if the elements are not mutually comparable
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3)
            throws NullPointerException, ClassCastException {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3)), true);
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
     * @param <E> the element type; must extend {@code Comparable<? super E>}
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     * @throws ClassCastException if the elements are not mutually comparable
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4)
            throws NullPointerException, ClassCastException {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4)), true);
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
     * @param <E> the element type; must extend {@code Comparable<? super E>}
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     * @throws ClassCastException if the elements are not mutually comparable
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5)
            throws NullPointerException, ClassCastException {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5)), true);
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
     * @param <E> the element type; must extend {@code Comparable<? super E>}
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     * @throws ClassCastException if the elements are not mutually comparable
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6)
            throws NullPointerException, ClassCastException {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6)), true);
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
     * @param <E> the element type; must extend {@code Comparable<? super E>}
     * @param e1 the first element
     * @param e2 the second element
     * @param e3 the third element
     * @param e4 the fourth element
     * @param e5 the fifth element
     * @param e6 the sixth element
     * @param e7 the seventh element
     * @return an {@code ImmutableSortedSet} containing the specified elements in sorted order
     * @throws NullPointerException if any element is {@code null}
     * @throws ClassCastException if the elements are not mutually comparable
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7)
            throws NullPointerException, ClassCastException {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7)), true);
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
     * @param <E> the element type; must extend {@code Comparable<? super E>}
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
     * @throws ClassCastException if the elements are not mutually comparable
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7,
            final E e8) throws NullPointerException, ClassCastException {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8)), true);
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
     * @param <E> the element type; must extend {@code Comparable<? super E>}
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
     * @throws ClassCastException if the elements are not mutually comparable
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7,
            final E e8, final E e9) throws NullPointerException, ClassCastException {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9)), true);
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
     * @param <E> the element type; must extend {@code Comparable<? super E>}
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
     * @throws ClassCastException if the elements are not mutually comparable
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7,
            final E e8, final E e9, final E e10) throws NullPointerException, ClassCastException {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10)), true);
    }

    /**
     * This method is deprecated and will always throw an {@link UnsupportedOperationException}.
     *
     * <p>{@code ImmutableSet.builder()} is a static method and is therefore reachable through this
     * subclass's name, where {@code ImmutableSortedSet.builder().build()} would silently produce an
     * <i>unsorted</i> {@link ImmutableSet} in insertion order. This overload hides it so the mistake fails
     * loudly. Collect the elements into a {@link java.util.TreeSet} and pass it to
     * {@link #copyOf(Collection)}, or use one of the {@code of(...)} factories.</p>
     *
     * <p>The return type must stay {@code ImmutableSet.Builder} to legally hide the superclass method;
     * it never returns.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet.builder();   // throws UnsupportedOperationException
     *
     * // instead:
     * ImmutableSortedSet<String> sorted = ImmutableSortedSet.copyOf(N.asList("b", "a"));
     * }</pre>
     *
     * @param <E> the element type
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated use {@link #copyOf(Collection)}, or an {@code of(...)} factory.
     */
    @Deprecated
    public static <E> ImmutableSet.Builder<E> builder() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is deprecated and will always throw an {@link UnsupportedOperationException}.
     *
     * <p>See {@link #builder()}: the inherited {@code ImmutableSet.builder(Set)} would silently produce an
     * unsorted {@link ImmutableSet}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet.builder(new TreeSet<String>());   // throws UnsupportedOperationException
     * }</pre>
     *
     * @param <E> the element type
     * @param holder ignored
     * @return never returns normally
     * @throws UnsupportedOperationException always
     * @deprecated use {@link #copyOf(Collection)}, or an {@code of(...)} factory.
     */
    @Deprecated
    public static <E> ImmutableSet.Builder<E> builder(final Set<E> holder) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns an ImmutableSortedSet containing the elements of the specified array in natural order.
     * Duplicate elements are removed. If the array is {@code null} or empty, the cached empty set is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<String> set = ImmutableSortedSet.copyOf(new String[] { "charlie", "alpha", "beta", "alpha" });
     * System.out.println(set);   // prints [alpha, beta, charlie]
     * }</pre>
     *
     * @param <E> the type of elements, which must be mutually comparable
     * @param a the array whose elements are to be copied
     * @return an ImmutableSortedSet containing the array elements
     * @throws NullPointerException if the array contains a {@code null} element
     * @throws ClassCastException if the elements are not mutually comparable
     * @see #copyOf(Collection)
     */
    public static <E> ImmutableSortedSet<E> copyOf(final E[] a) throws NullPointerException, ClassCastException {
        return N.isEmpty(a) ? empty() : new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(a)), true);
    }

    /**
     * Returns an ImmutableSortedSet containing the elements of the specified collection.
     * If the provided collection is already an instance of ImmutableSortedSet that owns its backing storage
     * (one produced by {@code of(...)}, {@code copyOf(...)} or {@link #empty()}), it is directly returned;
     * a {@link #wrap(SortedSet)}-created view is copied like any other collection.
     * If the provided collection is {@code null} or empty, an empty ImmutableSortedSet is returned.
     * Otherwise, a new ImmutableSortedSet is created with the elements of the provided collection.
     *
     * <p>A {@link SortedSet} source retains its comparator, including when it is empty; its elements
     * need not implement {@link Comparable} when that comparator supports them. Other collections use
     * natural ordering and throw {@link ClassCastException} if their elements are not mutually comparable.</p>
     *
     * <p><b>Note:</b> a returned same instance is independent of further <i>modification</i>, not of the
     * source's <i>memory</i>. This holds for every derived view of an owning set, not just a range:
     * {@code subSet}/{@code headSet}/{@code tailSet}/{@code reversed}, and the {@code descendingSet},
     * {@code navigableKeySet} and {@code descendingKeySet} views of the navigable subtypes, all own their
     * backing storage too, so they are returned unchanged - and, like every {@code SortedSet} sub-view, each
     * keeps its whole parent reachable. For a key-set view that parent is the entire map, its values included.
     * Wrap the view in a fresh set ({@code copyOf(new TreeSet<>(view))}) when a small view of a large source
     * must stop retaining it.</p>
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
     * @return an {@code ImmutableSortedSet} containing the elements of the specified collection, or the same instance
     *         if it is already an {@code ImmutableSortedSet} that owns its backing storage. The comparator of a
     *         {@code SortedSet} source is retained even when the source is empty; a {@code null} or empty
     *         non-sorted source returns the shared empty instance.
     * @throws NullPointerException if the collection contains a {@code null} element and natural ordering is used
     * @throws ClassCastException if the elements are not mutually comparable (when the source collection is not a {@code SortedSet})
     * @see #wrap(SortedSet)
     */
    public static <E> ImmutableSortedSet<E> copyOf(final Collection<? extends E> c) throws NullPointerException, ClassCastException {
        if (c instanceof ImmutableSortedSet && ((ImmutableSortedSet<E>) c).ownsBacking) {
            return (ImmutableSortedSet<E>) c;
        } else if (c instanceof SortedSet sortedSet) {
            return new ImmutableSortedSet<>(new TreeSet<>(sortedSet), true);
        } else if (N.isEmpty(c)) {
            return empty();
        } else {
            return new ImmutableSortedSet<>(new TreeSet<>(c), true);
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
     * immutability contract. Use {@link #copyOf(Collection)} for a truly independent immutable copy.</p>
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
     * @throws NullPointerException if an endpoint is null and the backing set rejects null endpoints
     * @throws ClassCastException if the endpoints cannot be compared with each other, or an endpoint cannot be compared with a backing range bound
     *         using the configured comparator or natural ordering
     * @throws IllegalArgumentException if {@code fromElement} is greater than {@code toElement}; or if this set itself has a restricted range, and
     *         {@code fromElement} or {@code toElement} lies outside the bounds of the range.
     */
    @Override
    public ImmutableSortedSet<E> subSet(final E fromElement, final E toElement) throws NullPointerException, ClassCastException, IllegalArgumentException {
        // A range/derived view is a window onto this instance's own backing storage, so it is exactly
        // as stable as this instance is: an owning parent yields an owning view, a wrap()-backed one a live view.
        return new ImmutableSortedSet<>(sortedSet.subSet(fromElement, toElement), ownsBacking);
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
     * @throws NullPointerException if {@code toElement} is null and the backing set rejects null endpoints
     * @throws ClassCastException if the endpoint is incompatible with the ordering, or cannot be compared with a backing range bound
     * @throws IllegalArgumentException if this set itself has a restricted range, and {@code toElement} lies outside the bounds of the range.
     */
    @Override
    public ImmutableSortedSet<E> headSet(final E toElement) throws NullPointerException, ClassCastException, IllegalArgumentException {
        // A range/derived view is a window onto this instance's own backing storage, so it is exactly
        // as stable as this instance is: an owning parent yields an owning view, a wrap()-backed one a live view.
        return new ImmutableSortedSet<>(sortedSet.headSet(toElement), ownsBacking);
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
     * @throws NullPointerException if {@code fromElement} is null and the backing set rejects null endpoints
     * @throws ClassCastException if the endpoint is incompatible with the ordering, or cannot be compared with a backing range bound
     * @throws IllegalArgumentException if this set itself has a restricted range, and {@code fromElement} lies outside the bounds of the range.
     */
    @Override
    public ImmutableSortedSet<E> tailSet(final E fromElement) throws NullPointerException, ClassCastException, IllegalArgumentException {
        // A range/derived view is a window onto this instance's own backing storage, so it is exactly
        // as stable as this instance is: an owning parent yields an owning view, a wrap()-backed one a live view.
        return new ImmutableSortedSet<>(sortedSet.tailSet(fromElement), ownsBacking);
    }

    /**
     * Returns an immutable view of this set with its elements in reverse order.
     * The returned set is backed by this set, so it remains immutable, and it has an ordering equivalent to
     * {@link java.util.Collections#reverseOrder(java.util.Comparator) Collections.reverseOrder(comparator())}.
     *
     * <p>This narrows the {@link java.util.SortedSet#reversed()} default, which would otherwise hand back a
     * plain JDK view that is neither an {@code ImmutableSortedSet} nor an {@link Immutable}, unlike the
     * reversed views of {@link ImmutableList}, {@link ImmutableNavigableSet} and {@link ImmutableNavigableMap}.</p>
     *
     * <p>A fresh view is returned on every call, so {@code set.reversed().reversed()} is {@code equals} to
     * {@code set} but is not the same instance - the rule {@link ImmutableNavigableSet#descendingSet()}
     * already follows.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableSortedSet<Integer> set = ImmutableSortedSet.of(1, 2, 3);
     * ImmutableSortedSet<Integer> reversed = set.reversed();
     * System.out.println(reversed);   // prints [3, 2, 1]
     * }</pre>
     *
     * @return an immutable view of this set with its elements in reverse order
     */
    @Override
    public ImmutableSortedSet<E> reversed() {
        // A range/derived view is a window onto this instance's own backing storage, so it is exactly
        // as stable as this instance is: an owning parent yields an owning view, a wrap()-backed one a live view.
        return new ImmutableSortedSet<>(sortedSet.reversed(), ownsBacking);
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
    public E first() throws NoSuchElementException {
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
    public E last() throws NoSuchElementException {
        return sortedSet.last();
    }

    /**
     * This operation is not supported by ImmutableSortedSet.
     * Attempting to call this method will always throw an UnsupportedOperationException,
     * including on an empty set.
     *
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableSortedSet does not support modification operations.
     */
    @Deprecated
    @Override
    public E removeFirst() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableSortedSet.
     * Attempting to call this method will always throw an UnsupportedOperationException,
     * including on an empty set.
     *
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableSortedSet does not support modification operations.
     */
    @Deprecated
    @Override
    public E removeLast() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}

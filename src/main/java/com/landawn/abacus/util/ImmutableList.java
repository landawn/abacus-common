/*
 * Copyright (C) 2016 HaiYang Li
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * A read-only implementation of the {@link List} interface.
 * Its contents cannot be modified through the {@code ImmutableList} API.
 * All mutating operations (add, remove, set, sort, etc.) will throw {@link UnsupportedOperationException}.
 *
 * <p>Instances that own their backing storage - those from {@link #of(Object)}, {@link #copyOf(Collection)},
 * {@link #copyOf(Object[])}, {@link #empty()}, a consumed no-argument {@link #builder()} and
 * {@code toImmutableList()} on an {@link ObjIterator} (or on any other iterator type in this package), plus
 * the {@link #subList(int, int)} and {@link #reversed()} views of such a list - are immutable and thread-safe. Instances created by
 * {@link #wrap(List)} or by a {@link Builder} over a caller-supplied holder are read-only <i>views</i>: they
 * reflect external changes to the backing list and have that list's thread-safety characteristics. {@link #copyOf(Collection)} always
 * returns an owning instance, so it turns a view into an independent value.
 *
 * <p>This class provides several static factory methods for creating instances:
 * <ul>
 *   <li>{@link #empty()} - returns an empty list</li>
 *   <li>{@link #of(Object)} (and arity-overloads up to ten elements) - creates lists with specific elements</li>
 *   <li>{@link #copyOf(Collection)} - creates a defensive copy from another collection</li>
 *   <li>{@link #wrap(List)} - wraps an existing list (changes to the underlying list will be reflected)</li>
 *   <li>{@link #builder()} - provides a builder for constructing lists incrementally</li>
 * </ul>
 *
 * <p>The implementation maintains the iteration order of elements as they were added.
 * All elements (including null) are supported.
 *
 * <p>Additional features:
 * <ul>
 *   <li>{@link #reversed()} - returns a reversed view of the list</li>
 *   <li>{@link #subList(int, int)} - returns an immutable view of a portion of the list</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create using factory methods
 * ImmutableList<String> list1 = ImmutableList.of("a", "b", "c");
 *
 * // Create from existing collection
 * List<Integer> mutable = Arrays.asList(1, 2, 3);
 * ImmutableList<Integer> list2 = ImmutableList.copyOf(mutable);
 *
 * // Create using builder
 * List<String> moreElements = Arrays.asList("fourth", "fifth");
 * ImmutableList<String> list3 = ImmutableList.<String>builder()
 *     .add("first")
 *     .add("second", "third")
 *     .addAll(moreElements)
 *     .build();
 *
 * // Use reverse view
 * List<Integer> reversedView = ImmutableList.of(1, 2, 3).reversed();
 * // reversedView contains [3, 2, 1]
 * }</pre>
 *
 * @param <E> the type of elements in this list
 * @see List
 * @see ImmutableCollection
 */
@com.landawn.abacus.annotation.Immutable
@SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
@SuppressWarnings("java:S2160")
public sealed class ImmutableList<E> extends ImmutableCollection<E> implements List<E>
        permits ImmutableList.ReverseImmutableList, ImmutableList.RandomAccessImmutableList {

    // Collections.emptyList()'s spliterator is Spliterators.emptySpliterator(), which reports only
    // SIZED|SUBSIZED and so drops the ORDERED that List.spliterator() promises; an operator that intersects
    // characteristics (Stream.concat) then yields an unordered stream, making a later findFirst() on a
    // parallel stream non-deterministic. Do NOT "simplify" this to List.of(): its contains(null) and
    // indexOf(null) throw NullPointerException, which this empty list must not.
    @SuppressWarnings("rawtypes")
    private static final ImmutableList EMPTY = create(Collections.unmodifiableList(new ArrayList<>(0)), true, true);

    /**
     * The unmodifiable {@code List} view that backs this instance; the same object the
     * superclass holds as its backing collection, kept here so that list-specific operations
     * avoid a cast on every call. Never {@code null}.
     */
    final List<E> list;

    /**
     * Constructs a non-owning ImmutableList backed by the provided list.
     * The backing list is always exposed through an unmodifiable view; its concrete class name
     * is not treated as evidence that it is immutable.
     *
     * @param list the list of elements to be included in this ImmutableList.
     * @throws NullPointerException if {@code list} is {@code null}
     */
    ImmutableList(final List<? extends E> list) throws NullPointerException {
        this(list, false, false);
    }

    /**
     * Constructs a non-owning ImmutableList backed by the provided list.
     * If {@code isUnmodifiable} is {@code false}, the list is wrapped in an unmodifiable view.
     *
     * <p><b>The flag is {@code isUnmodifiable}, not ownership.</b> The sorted members of this family
     * ({@code ImmutableSortedSet}, {@code ImmutableSortedMap}, {@code ImmutableNavigableMap},
     * {@code ImmutableNavigableSet}, {@code ImmutableBiMap}) spell their two-argument constructor
     * {@code (backing, ownsBacking)} instead, so the same call shape means the opposite thing there. This
     * overload is safe only because {@code ImmutableList} is {@code sealed} and neither permitted subclass
     * declares a two-argument constructor; {@code ImmutableSet}, {@code ImmutableMap} and
     * {@code AbstractImmutableMap} deliberately have no two-argument form for that reason. Pass all three
     * arguments explicitly in any new code.</p>
     *
     * @param list the list of elements to be included in this ImmutableList.
     * @param isUnmodifiable {@code true} if the provided list is already unmodifiable and does not need wrapping.
     * @throws NullPointerException if {@code list} is {@code null} and {@code isUnmodifiable} is false
     */
    ImmutableList(final List<? extends E> list, final boolean isUnmodifiable) throws NullPointerException {
        this(list, isUnmodifiable, false);
    }

    /**
     * Constructs an ImmutableList backed by the provided list.
     * If {@code isUnmodifiable} is {@code false}, the list is wrapped in an unmodifiable view.
     *
     * @param list the list of elements to be included in this ImmutableList.
     * @param isUnmodifiable {@code true} if the provided list is already unmodifiable and does not need wrapping.
     * @param ownsBacking {@code true} only if no other modifiable reference to {@code list} survives this call;
     *        see {@link ImmutableCollection#ownsBacking}.
     * @throws NullPointerException if {@code list} is {@code null} and {@code isUnmodifiable} is false
     */
    @SuppressFBWarnings("BC_BAD_CAST_TO_ABSTRACT_COLLECTION")
    ImmutableList(final List<? extends E> list, final boolean isUnmodifiable, final boolean ownsBacking) throws NullPointerException {
        super(isUnmodifiable ? list : Collections.unmodifiableList(list), ownsBacking);
        this.list = (List<E>) coll;
    }

    /**
     * Creates an {@code ImmutableList} over the given backing list, choosing the
     * {@link RandomAccess}-implementing variant when the backing list itself is {@code RandomAccess}.
     *
     * <p>{@code Collections.unmodifiableList} already preserves {@code RandomAccess}, so the marker is
     * checked on the supplied list. Keeping the marker matters because JDK algorithms
     * ({@code Collections.binarySearch}, {@code Collections.indexOfSubList}, ...) and the widespread
     * {@code if (list instanceof RandomAccess)} idiom otherwise fall back to their linear
     * iterator-based paths on a list that is really array-backed.</p>
     *
     * @param <E> the element type
     * @param list the backing list
     * @param isUnmodifiable {@code true} if {@code list} is already unmodifiable
     * @param ownsBacking see {@link ImmutableCollection#ownsBacking}
     * @return a new {@code ImmutableList} over {@code list}
     * @throws NullPointerException if {@code list} is {@code null} and {@code isUnmodifiable} is false
     */
    static <E> ImmutableList<E> create(final List<? extends E> list, final boolean isUnmodifiable, final boolean ownsBacking) throws NullPointerException {
        return list instanceof RandomAccess ? new RandomAccessImmutableList<>(list, isUnmodifiable, ownsBacking)
                : new ImmutableList<>(list, isUnmodifiable, ownsBacking);
    }

    /**
     * Returns an empty ImmutableList. This method always returns the same cached instance,
     * making it memory efficient for representing empty lists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> empty = ImmutableList.empty();
     * System.out.println(empty.size());      // prints 0
     * System.out.println(empty.isEmpty());   // prints true
     * }</pre>
     *
     * @param <E> the type of elements in the list.
     * @return an empty ImmutableList instance.
     */
    public static <E> ImmutableList<E> empty() {
        return EMPTY;
    }

    /**
     * Returns an ImmutableList containing a single element.
     * The returned list is immutable and will have a size of 1.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<Integer> single = ImmutableList.of(42);
     * // single.add(43);   // Would throw UnsupportedOperationException
     * }</pre>
     *
     * @param <E> the type of the element.
     * @param e1 the single element to be contained in the ImmutableList.
     * @return an ImmutableList containing only the specified element.
     */
    public static <E> ImmutableList<E> of(final E e1) {
        return create(Array.asList(e1), false, true);
    }

    /**
     * Returns an ImmutableList containing exactly two elements in the order provided.
     * The returned list is immutable and will have a size of 2.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> pair = ImmutableList.of("first", "second");
     * }</pre>
     *
     * @param <E> the type of elements.
     * @param e1 the first element.
     * @param e2 the second element.
     * @return an ImmutableList containing the specified elements in order.
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2) {
        return create(Array.asList(e1, e2), false, true);
    }

    /**
     * Returns an ImmutableList containing exactly three elements in the order provided.
     * The returned list is immutable and will have a size of 3.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<Integer> triple = ImmutableList.of(1, 2, 3);
     * }</pre>
     *
     * @param <E> the type of elements.
     * @param e1 the first element.
     * @param e2 the second element.
     * @param e3 the third element.
     * @return an ImmutableList containing the specified elements in order.
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3) {
        return create(Array.asList(e1, e2, e3), false, true);
    }

    /**
     * Returns an ImmutableList containing exactly four elements in the order provided.
     * The returned list is immutable and will have a size of 4.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> seasons = ImmutableList.of("Spring", "Summer", "Fall", "Winter");
     * }</pre>
     *
     * @param <E> the type of elements.
     * @param e1 the first element.
     * @param e2 the second element.
     * @param e3 the third element.
     * @param e4 the fourth element.
     * @return an ImmutableList containing the specified elements in order.
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4) {
        return create(Array.asList(e1, e2, e3, e4), false, true);
    }

    /**
     * Returns an ImmutableList containing exactly five elements in the order provided.
     * The returned list is immutable and will have a size of 5.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<Integer> primes = ImmutableList.of(2, 3, 5, 7, 11);
     * }</pre>
     *
     * @param <E> the type of elements.
     * @param e1 the first element.
     * @param e2 the second element.
     * @param e3 the third element.
     * @param e4 the fourth element.
     * @param e5 the fifth element.
     * @return an ImmutableList containing the specified elements in order.
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5) {
        return create(Array.asList(e1, e2, e3, e4, e5), false, true);
    }

    /**
     * Returns an ImmutableList containing exactly six elements in the order provided.
     * The returned list is immutable and will have a size of 6.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> days = ImmutableList.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat");
     * }</pre>
     *
     * @param <E> the type of elements.
     * @param e1 the first element.
     * @param e2 the second element.
     * @param e3 the third element.
     * @param e4 the fourth element.
     * @param e5 the fifth element.
     * @param e6 the sixth element.
     * @return an ImmutableList containing the specified elements in order.
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6) {
        return create(Array.asList(e1, e2, e3, e4, e5, e6), false, true);
    }

    /**
     * Returns an ImmutableList containing exactly seven elements in the order provided.
     * The returned list is immutable and will have a size of 7.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> weekDays = ImmutableList.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
     * }</pre>
     *
     * @param <E> the type of the elements.
     * @param e1 the first element.
     * @param e2 the second element.
     * @param e3 the third element.
     * @param e4 the fourth element.
     * @param e5 the fifth element.
     * @param e6 the sixth element.
     * @param e7 the seventh element.
     * @return an ImmutableList containing the specified elements in order.
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7) {
        return create(Array.asList(e1, e2, e3, e4, e5, e6, e7), false, true);
    }

    /**
     * Returns an ImmutableList containing exactly eight elements in the order provided.
     * The returned list is immutable and will have a size of 8.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<Integer> fibonacci = ImmutableList.of(1, 1, 2, 3, 5, 8, 13, 21);
     * }</pre>
     *
     * @param <E> the type of the elements.
     * @param e1 the first element.
     * @param e2 the second element.
     * @param e3 the third element.
     * @param e4 the fourth element.
     * @param e5 the fifth element.
     * @param e6 the sixth element.
     * @param e7 the seventh element.
     * @param e8 the eighth element.
     * @return an ImmutableList containing the specified elements in order.
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8) {
        return create(Array.asList(e1, e2, e3, e4, e5, e6, e7, e8), false, true);
    }

    /**
     * Returns an ImmutableList containing exactly nine elements in the order provided.
     * The returned list is immutable and will have a size of 9.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<Integer> digits = ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
     * }</pre>
     *
     * @param <E> the type of the elements.
     * @param e1 the first element.
     * @param e2 the second element.
     * @param e3 the third element.
     * @param e4 the fourth element.
     * @param e5 the fifth element.
     * @param e6 the sixth element.
     * @param e7 the seventh element.
     * @param e8 the eighth element.
     * @param e9 the ninth element.
     * @return an ImmutableList containing the specified elements in order.
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8, final E e9) {
        return create(Array.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9), false, true);
    }

    /**
     * Returns an ImmutableList containing exactly ten elements in the order provided.
     * The returned list is immutable and will have a size of 10.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<Integer> digits = ImmutableList.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
     * }</pre>
     *
     * @param <E> the type of the elements.
     * @param e1 the first element.
     * @param e2 the second element.
     * @param e3 the third element.
     * @param e4 the fourth element.
     * @param e5 the fifth element.
     * @param e6 the sixth element.
     * @param e7 the seventh element.
     * @param e8 the eighth element.
     * @param e9 the ninth element.
     * @param e10 the tenth element.
     * @return an ImmutableList containing the specified elements in order.
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8, final E e9,
            final E e10) {
        return create(Array.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10), false, true);
    }

    /**
     * Returns an {@code ImmutableList} containing the elements of the specified array.
     * If the array is {@code null} or empty, an empty {@code ImmutableList} is returned.
     *
     * <p>Subsequent modifications to the original array do not affect the returned list.</p>
     *
     * <p>The iteration order of the resulting list matches the array's index order.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] array = { "a", "b", "c" };
     * ImmutableList<String> list = ImmutableList.copyOf(array);
     *
     * array[0] = "x";   // Does not affect list
     * // list => ["a", "b", "c"]
     * }</pre>
     *
     * @param <E> the element type
     * @param elements the array whose elements are to be placed into the {@code ImmutableList};
     *        may be {@code null} or empty
     * @return an {@code ImmutableList} containing the elements of {@code elements},
     *         or an empty list if {@code elements} is {@code null} or empty
     * @see #copyOf(Collection)
     */
    public static <E> ImmutableList<E> copyOf(final E[] elements) {
        if (N.isEmpty(elements)) {
            return empty();
        } else if (elements.length == 1) {
            return of(elements[0]);
        } else {
            // The clone is the defensive copy this method promises, and nothing else can reach it, so the
            // Arrays.asList view over it is owned storage. Routing through copyOf(Collection) instead would
            // build an intermediate ArrayList and then copy that a second time.
            return create(Array.asList(elements.clone()), false, true);
        }
    }

    /**
     * Returns an ImmutableList containing all elements from the provided collection.
     * If the collection is {@code null} or empty, an empty ImmutableList is returned.
     * Otherwise, a new ImmutableList is created with a defensive copy of the collection's elements.
     * The order of elements is preserved as provided by the collection's iterator.
     *
     * <p>The copy is skipped only when {@code c} is an {@code ImmutableList} that already owns its
     * backing storage - that is, one produced by {@code of(...)}, {@code copyOf(...)}, {@link #empty()},
     * by a consumed no-argument {@link #builder()}, by {@code toImmutableList()} on an {@link ObjIterator}
     * (or on any other iterator type in this package), or by {@link #subList(int, int)} or
     * {@link #reversed()} over such a list. An {@code ImmutableList} produced by
     * {@link #wrap(List)} or {@link #builder(List)} is a live view over
     * storage its creator may still modify, so it is copied like any other collection. The returned list
     * is therefore always an independent, stable value.</p>
     *
     * <p><b>Note:</b> "independent" means independent of further <i>modification</i>, not of the source's
     * <i>memory</i>. A sub-range of an owning list ({@link #subList(int, int)}) owns its backing storage too,
     * so it is returned unchanged - and, like every {@code List} sub-view, it keeps the whole parent list
     * reachable. Wrap the range in a fresh collection ({@code ImmutableList.copyOf(new ArrayList<>(sub))})
     * when a small range of a large list must stop retaining it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> mutable = new ArrayList<>(Arrays.asList(1, 2, 3));
     * ImmutableList<Integer> immutable = ImmutableList.copyOf(mutable);
     * mutable.add(4);   // Does not affect immutable
     *
     * ImmutableList<Integer> view = ImmutableList.wrap(mutable);
     * ImmutableList<Integer> copy = ImmutableList.copyOf(view);
     * mutable.add(5);   // Visible through view, but NOT through copy
     * }</pre>
     *
     * @param <E> the type of elements in the collection.
     * @param c the collection whose elements are to be placed into the {@code ImmutableList};
     *        may be {@code null} or empty.
     * @return the same instance if {@code c} is already an {@code ImmutableList} that owns its backing storage;
     *         an empty {@code ImmutableList} if {@code c} is {@code null} or empty;
     *         otherwise a new {@code ImmutableList} containing a defensive copy of the collection's elements.
     * @see #copyOf(Object[])
     * @see #wrap(List)
     */
    public static <E> ImmutableList<E> copyOf(final Collection<? extends E> c) {
        if (c instanceof ImmutableList && ((ImmutableList<E>) c).ownsBacking) {
            return (ImmutableList<E>) c;
        } else if (N.isEmpty(c)) {
            return empty();
        } else {
            return create(new ArrayList<>(c), false, true);
        }
    }

    /**
     * Wraps the provided list into an ImmutableList without copying the elements.
     * If the provided list is already an ImmutableList, it is returned directly.
     * If the list is {@code null}, an empty ImmutableList is returned.
     *
     * <p><b>Warning:</b> This method does not create a defensive copy. Changes to the
     * underlying List will be reflected in the returned ImmutableList, which
     * violates the immutability contract. For a truly independent immutable copy, use
     * {@link #copyOf(Collection)} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> mutable = new ArrayList<>();
     * mutable.add("initial");
     *
     * ImmutableList<String> wrapped = ImmutableList.wrap(mutable);
     * mutable.add("added later");           // This WILL be visible in wrapped!
     * System.out.println(wrapped.get(1));   // prints "added later"
     * }</pre>
     *
     * @param <E> the type of elements in the list.
     * @param list the list to be wrapped into an {@code ImmutableList}; may be {@code null}.
     * @return the same instance if {@code list} is already an {@code ImmutableList};
     *         an empty {@code ImmutableList} if {@code list} is {@code null};
     *         otherwise an {@code ImmutableList} view backed by {@code list}.
     * @see #copyOf(Collection)
     */
    @Beta
    public static <E> ImmutableList<E> wrap(final List<? extends E> list) {
        if (list instanceof ImmutableList) {
            return (ImmutableList<E>) list;
        } else if (list == null) {
            return empty();
        } else {
            return create(list, false, false);
        }
    }

    /**
     * This method is deprecated and will always throw an UnsupportedOperationException.
     * Use {@link #wrap(List)} or {@link #copyOf(Collection)} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Collection<String> c = Arrays.asList("a", "b");
     * ImmutableList.wrap(c);   // throws UnsupportedOperationException
     * }</pre>
     *
     * @param <E> the type of elements.
     * @param c the collection to wrap.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated Use {@link #wrap(List)} for lists or {@link #copyOf(Collection)} for general collections.
     */
    @Deprecated
    public static <E> ImmutableCollection<E> wrap(final Collection<? extends E> c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the element at the specified position in this list.
     * The index must be valid (between 0 inclusive and size() exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> list = ImmutableList.of("a", "b", "c");
     * String second = list.get(1);   // returns "b"
     * }</pre>
     *
     * @param index the index of the element to return (0-based).
     * @return the element at the specified position in this list.
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt;= size()).
     * @see List#get(int)
     */
    @Override
    public E get(final int index) throws IndexOutOfBoundsException {
        return list.get(index);
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * or -1 if this list does not contain the element. The search is performed using
     * the equals() method of the element (or {@code null} comparison for {@code null} elements).
     * If multiple equal elements exist, the index of the first one is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> list = ImmutableList.of("a", "b", "c", "b");
     * int index = list.indexOf("b");      // returns 1
     * int notFound = list.indexOf("d");   // returns -1
     * }</pre>
     *
     * @param valueToFind the element to search for, may be {@code null}.
     * @return the index of the first occurrence of the specified element in this list,
     *         or -1 if this list does not contain the element.
     * @see List#indexOf(Object)
     */
    @Override
    public int indexOf(final Object valueToFind) {
        return list.indexOf(valueToFind);
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * or -1 if this list does not contain the element. The search is performed using
     * the equals() method of the element (or {@code null} comparison for {@code null} elements).
     * If multiple equal elements exist, the index of the last one is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> list = ImmutableList.of("a", "b", "c", "b");
     * int lastIndex = list.lastIndexOf("b");   // returns 3
     * }</pre>
     *
     * @param valueToFind the element to search for, may be {@code null}.
     * @return the index of the last occurrence of the specified element in this list,
     *         or -1 if this list does not contain the element.
     * @see List#lastIndexOf(Object)
     */
    @Override
    public int lastIndexOf(final Object valueToFind) {
        return list.lastIndexOf(valueToFind);
    }

    /**
     * Returns an immutable list iterator over the elements in this list in proper sequence.
     * The returned iterator does not support {@code remove()}, {@code set()}, or {@code add()}
     * and will throw {@link UnsupportedOperationException} if any of those operations are called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> list = ImmutableList.of("a", "b", "c");
     * ImmutableListIterator<String> iter = list.listIterator();
     * while (iter.hasNext()) {
     *     System.out.println(iter.next());
     * }
     * }</pre>
     *
     * @return an immutable list iterator over the elements in this list in proper sequence.
     * @see List#listIterator()
     */
    @Override
    public ImmutableListIterator<E> listIterator() {
        return ImmutableListIterator.of(list.listIterator());
    }

    /**
     * Returns an immutable list iterator over the elements in this list in proper sequence,
     * starting at the specified position in the list. The specified index indicates the first
     * element that would be returned by an initial call to next(). An initial call to previous()
     * would return the element with the specified index minus one.
     * The returned iterator does not support remove(), add(), or set() operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d");
     * ImmutableListIterator<String> iter = list.listIterator(2);
     * System.out.println(iter.next());       // prints "c"
     * System.out.println(iter.previous());   // prints "c" again
     * System.out.println(iter.previous());   // prints "b"
     * }</pre>
     *
     * @param index the index of the first element to be returned from the list iterator (by a call to next()).
     * @return an immutable list iterator over the elements in this list starting at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range (index &lt; 0 || index &gt; size()).
     * @see List#listIterator(int)
     */
    @Override
    public ImmutableListIterator<E> listIterator(final int index) throws IndexOutOfBoundsException {
        return ImmutableListIterator.of(list.listIterator(index));
    }

    /**
     * Returns an immutable view of the portion of this list between the specified
     * {@code fromIndex} (inclusive) and {@code toIndex} (exclusive). The returned sublist is backed
     * by this list. Since this list is immutable, the returned sublist is also immutable
     * and does not support any modification operations.
     *
     * <p>The semantics of the sublist are consistent with List.subList(), including
     * the behavior when fromIndex equals toIndex (returns an empty list).
     *
     * <p><b>Note:</b> like every {@code List} sub-view, the returned list is only valid while the list it
     * was taken from is not structurally modified through any other route. That cannot happen for a list
     * that owns its backing storage, but a {@link #wrap(List)}-backed list can be resized by whoever still
     * holds the wrapped list, and the sublist's behaviour is then undefined - the usual {@code ArrayList}
     * backing raises {@link java.util.ConcurrentModificationException} on the next access. Take the sublist
     * of a {@link #copyOf(Collection)} when the source may still change.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d", "e");
     * ImmutableList<String> sub = list.subList(1, 4);
     * // sub contains ["b", "c", "d"]
     * }</pre>
     *
     * @param fromIndex the low endpoint (inclusive) of the subList.
     * @param toIndex the high endpoint (exclusive) of the subList.
     * @return an immutable view of the specified range within this list.
     * @throws IndexOutOfBoundsException for an out-of-range endpoint index
     *         (fromIndex &lt; 0 || toIndex &gt; size).
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}.
     * @see List#subList(int, int)
     */
    @Override
    public ImmutableList<E> subList(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException, IllegalArgumentException {
        // Range-check here rather than leaving it to the backing list. java.util.List.subList specifies
        // IndexOutOfBoundsException for an inverted range, while every AbstractList-derived implementation
        // in the JDK actually raises IllegalArgumentException (Collections.subListRangeCheck). wrap() accepts
        // any List, so without this the exception type would depend on what the caller happened to wrap, and
        // would disagree with ReverseImmutableList.subList(), which performs exactly this check.
        checkSubListRange(fromIndex, toIndex, size());

        // A sub-view is a window onto this list's own backing storage, so it is exactly as stable as this
        // list is: an owning parent yields an owning sublist, a wrap()-backed parent yields a live one.
        return create(list.subList(fromIndex, toIndex), false, ownsBacking);
    }

    /**
     * Reproduces {@code java.util.AbstractList.subListRangeCheck} exactly, including its order: an
     * out-of-range endpoint is an {@link IndexOutOfBoundsException} while an inverted range is an
     * {@link IllegalArgumentException}. Shared by {@link #subList(int, int)} and
     * {@link ReverseImmutableList#subList(int, int)} so that both directions of a list agree on which
     * exception a bad range raises.
     *
     * @param fromIndex the low endpoint (inclusive) of the requested range
     * @param toIndex the high endpoint (exclusive) of the requested range
     * @param size the size of the list the range is taken from
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size}
     * @throws IllegalArgumentException if {@code fromIndex > toIndex}
     */
    static void checkSubListRange(final int fromIndex, final int toIndex, final int size) throws IndexOutOfBoundsException, IllegalArgumentException {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        }

        if (toIndex > size) {
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        }

        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
    }

    /**
     * This operation is not supported by ImmutableList.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param index ignored.
     * @param newElements ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableList does not support modification operations.
     */
    @Deprecated
    @Override
    public boolean addAll(final int index, final Collection<? extends E> newElements) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableList.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param index ignored.
     * @param element ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableList does not support modification operations.
     */
    @Deprecated
    @Override
    public E set(final int index, final E element) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableList.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param index ignored.
     * @param element ignored.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableList does not support modification operations.
     */
    @Deprecated
    @Override
    public void add(final int index, final E element) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableList.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param index ignored.
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableList does not support modification operations.
     */
    @Deprecated
    @Override
    public E remove(final int index) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableList.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param operator ignored, and may be {@code null}.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableList does not support modification operations.
     */
    @Deprecated
    @Override
    public void replaceAll(final UnaryOperator<E> operator) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableList.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param c ignored. A {@code null} comparator is accepted here as it is by
     *        {@link List#sort(Comparator)}, where it means the natural ordering.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableList does not support modification operations.
     */
    @Deprecated
    @Override
    public void sort(final Comparator<? super E> c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableList.
     * Attempting to call this method will always throw an UnsupportedOperationException,
     * including on an empty list.
     *
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableList does not support modification operations.
     */
    // Overrides List.removeFirst(), whose default checks isEmpty() BEFORE delegating to remove(0) and so
    // raises NoSuchElementException on an empty list instead of reporting that the list is read-only.
    // java.util.List.of() blocks it unconditionally for the same reason; this matches that.
    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException always, because this object does not support this mutation
     */
    @Deprecated
    @Override
    public E removeFirst() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableList.
     * Attempting to call this method will always throw an UnsupportedOperationException,
     * including on an empty list.
     *
     * @return never returns normally.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableList does not support modification operations.
     */
    // See removeFirst(): the inherited List default raises NoSuchElementException on an empty list.
    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException always, because this object does not support this mutation
     */
    @Deprecated
    @Override
    public E removeLast() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableList.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param e ignored; this list cannot be modified.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableList does not support modification operations.
     */
    // Overridden only so that a call is flagged at compile time like every other mutator on this class; the
    // inherited List.addFirst(E) default already fails at run time by delegating to add(0, e).
    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException always, because this object does not support this mutation
     */
    @Deprecated
    @Override
    public void addFirst(final E e) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * This operation is not supported by ImmutableList.
     * Attempting to call this method will always throw an UnsupportedOperationException.
     *
     * @param e ignored; this list cannot be modified.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableList does not support modification operations.
     */
    // See addFirst(E): the inherited List.addLast(E) default delegates to add(e) and so is already blocked
    // at run time; this override adds the missing compile-time deprecation warning.
    /**
     * {@inheritDoc}
     * @throws UnsupportedOperationException always, because this object does not support this mutation
     */
    @Deprecated
    @Override
    public void addLast(final E e) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a view of this immutable list in reverse order. For example,
     * {@code ImmutableList.of(1, 2, 3).reversed()} returns a list containing {@code [3, 2, 1]}.
     * The returned list is backed by this list, so it's still immutable. The reverse operation
     * is efficient and does not copy elements: the view reads the backing list's current size on
     * each new operation, so for a list created via {@link #wrap(List)} whose backing list is later
     * resized, the view keeps presenting the current contents in reverse order.
     * Iterators retain the backing iterator's modification behavior (for example, fail-fast or
     * snapshot iteration). Traversal uses a backing list iterator and is linear for linked lists.
     *
     * <p>If this list owns its backing storage (it came from {@code of(...)}, {@code copyOf(...)},
     * {@link #empty()}, a consumed no-argument {@link #builder()}, {@code toImmutableList()} on an
     * {@link ObjIterator}, or {@link #subList(int, int)} over such a list) and has one or zero elements,
     * this same instance is returned, because such a
     * list can never differ from its own reverse. A {@link #wrap(List)}-backed list always gets a real
     * reversed view, since its backing list may still grow.
     * Calling reversed() on an already reversed list returns the original list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> reversed = ImmutableList.of("a", "b", "c").reversed();
     * // reversed contains ["c", "b", "a"]
     * }</pre>
     *
     * @return an immutable view of this list with elements in reverse order; this same instance if this
     *         list owns its backing storage and has zero or one element.
     */
    @Override
    public ImmutableList<E> reversed() {
        // The identity shortcut is only sound for a stable backing list: a wrap()-backed singleton can
        // grow to two elements, at which point "this" would no longer be a reversed view of itself.
        return (ownsBacking && size() <= 1) ? this : new ReverseImmutableList<>(this);
    }

    /**
     * An immutable list that presents the elements of a backing {@link ImmutableList} in reverse order.
     * Instances are created by {@link ImmutableList#reversed()} and are themselves immutable.
     * All mutating operations throw {@link UnsupportedOperationException}.
     *
     * <p>New operations read the forward list's current size, so the view tracks a
     * {@link ImmutableList#wrap(List)}-backed list that is resized after the view was created.
     * Iterators follow the backing iterator's modification behavior.</p>
     *
     * <p>Unlike {@link RandomAccessImmutableList}, this class never implements {@link RandomAccess}, even
     * over a random-access forward list. Positional access is still constant-time whenever the forward
     * list's is; only the marker interface (and the iterator-versus-index choice some algorithms make
     * from it) is absent.</p>
     *
     * @param <E> the type of elements in this list
     */
    @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
    static final class ReverseImmutableList<E> extends ImmutableList<E> {
        private final ImmutableList<E> forwardList;

        /**
         * Constructs a reversed view of the given immutable list.
         *
         * @param backingList the list whose elements this view presents in reverse order
         * @throws NullPointerException if {@code backingList} is {@code null}
         */
        ReverseImmutableList(final ImmutableList<E> backingList) throws NullPointerException {
            super(backingList.list, true, backingList.ownsBacking);
            forwardList = backingList;
        }

        /**
         * Returns the forward (non-reversed) list that this reversed view is backed by.
         *
         * @return the original forward {@code ImmutableList}
         */
        @Override
        public ImmutableList<E> reversed() {
            return forwardList;
        }

        /**
         * Returns {@code true} if this list contains the specified element,
         * delegating the check to the forward list.
         *
         * @param object the element whose presence is to be tested, may be {@code null}
         * @return {@code true} if this list contains the specified element
         */
        @Override
        public boolean contains(final Object object) {
            return forwardList.contains(object);
        }

        /**
         * Returns the index of the first occurrence of the specified element in this reversed list,
         * or {@code -1} if the element is not present.
         * Because the iteration order is reversed, this corresponds to the last occurrence
         * in the forward list.
         *
         * @param object the element to search for, may be {@code null}
         * @return the index of the first occurrence in this reversed list, or {@code -1} if not found
         */
        @Override
        public int indexOf(final Object object) {
            @SuppressWarnings("SuspiciousMethodCalls")
            final int index = forwardList.lastIndexOf(object);
            final int size = size();

            return (index >= 0 && index < size) ? reverseIndex(index, size) : -1;
        }

        /**
         * Returns the index of the last occurrence of the specified element in this reversed list,
         * or {@code -1} if the element is not present.
         * Because the iteration order is reversed, this corresponds to the first occurrence
         * in the forward list.
         *
         * @param object the element to search for, may be {@code null}
         * @return the index of the last occurrence in this reversed list, or {@code -1} if not found
         */
        @Override
        public int lastIndexOf(final Object object) {
            @SuppressWarnings("SuspiciousMethodCalls")
            final int index = forwardList.indexOf(object);
            final int size = size();

            return (index >= 0 && index < size) ? reverseIndex(index, size) : -1;
        }

        /**
         * Returns an immutable reversed view of the specified range of this list.
         * The returned sublist reflects the reversed ordering of elements in this list.
         *
         * @param fromIndex low endpoint (inclusive) of the sublist
         * @param toIndex high endpoint (exclusive) of the sublist
         * @return an immutable reversed view of the specified range
         * @throws IndexOutOfBoundsException if {@code fromIndex < 0} or {@code toIndex > size()}
         * @throws IllegalArgumentException if {@code fromIndex > toIndex}
         */
        @Override
        public ImmutableList<E> subList(final int fromIndex, final int toIndex) throws IndexOutOfBoundsException, IllegalArgumentException {
            final int size = size();

            // ImmutableList.subList() applies the same check, so both directions of a list agree on which
            // exception a bad range raises.
            checkSubListRange(fromIndex, toIndex, size);

            return forwardList.subList(reversePosition(toIndex, size), reversePosition(fromIndex, size)).reversed();
        }

        /**
         * Returns the element at the specified position in this reversed list.
         * Index {@code 0} corresponds to the last element of the forward list.
         *
         * @param index the position of the element to return (0-based)
         * @return the element at the specified position
         * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
         */
        @Override
        public E get(final int index) throws IndexOutOfBoundsException {
            final int size = size();

            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + size);
            }

            return forwardList.get(reverseIndex(index, size));
        }

        /**
         * Returns the number of elements in this list, which is the current size of the forward list.
         *
         * @return the number of elements
         */
        @Override
        public int size() {
            // Read through instead of caching at construction: a wrap()-backed forward list can be
            // resized after this view is created, and a stale size would silently truncate (or over-run)
            // every operation here. Callers below snapshot this once so a single call stays consistent.
            return forwardList.size();
        }

        /**
         * Returns an iterator over the elements of this reversed list in reverse order
         * (i.e., from the last element of the forward list to the first).
         *
         * @return an {@link ObjIterator} over the elements in reverse order
         */
        @Override
        public ObjIterator<E> iterator() {
            return listIterator(0);
        }

        /**
         * Returns an immutable list iterator over the elements in this reversed list, starting at position 0.
         *
         * @return an immutable list iterator positioned at the start of this reversed list
         */
        @Override
        public ImmutableListIterator<E> listIterator() {
            return listIterator(0);
        }

        /**
         * Returns an immutable list iterator over the elements in this reversed list,
         * starting at the specified position.
         *
         * @param index the index of the first element to be returned by the iterator's {@code next()} call
         * @return an immutable list iterator starting at the given position
         * @throws IndexOutOfBoundsException if {@code index < 0 || index > size()}
         */
        @Override
        public ImmutableListIterator<E> listIterator(final int index) throws IndexOutOfBoundsException {
            final int size = size();

            // Reported the way the forward list reports it (AbstractList.rangeCheckForAdd): a zero-length
            // *range* check would describe a bad index as "Start Index i with size 0", which no caller asked for.
            if (index < 0 || index > size) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
            }

            final ImmutableListIterator<E> forward = forwardList.listIterator(reversePosition(index, size));

            return ImmutableListIterator.of(new java.util.ListIterator<>() {
                @Override
                public boolean hasNext() {
                    return forward.hasPrevious();
                }

                /**
                 * {@inheritDoc}
                 * @throws NoSuchElementException if no next element remains in this iterator
                 */
                @Override
                public E next() throws NoSuchElementException {
                    return forward.previous();
                }

                @Override
                public boolean hasPrevious() {
                    return forward.hasNext();
                }

                /**
                 * {@inheritDoc}
                 * @throws NoSuchElementException if no previous element remains in this iterator
                 */
                @Override
                public E previous() throws NoSuchElementException {
                    return forward.next();
                }

                @Override
                public int nextIndex() {
                    return size - forward.nextIndex();
                }

                @Override
                public int previousIndex() {
                    return nextIndex() - 1;
                }

                /**
                 * {@inheritDoc}
                 * @throws UnsupportedOperationException always, because this object does not support this mutation
                 */
                @Override
                public void remove() throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }

                /**
                 * {@inheritDoc}
                 * @throws UnsupportedOperationException always, because this object does not support this mutation
                 */
                @Override
                public void set(final E e) throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }

                /**
                 * {@inheritDoc}
                 * @throws UnsupportedOperationException always, because this object does not support this mutation
                 */
                @Override
                public void add(final E e) throws UnsupportedOperationException {
                    throw new UnsupportedOperationException();
                }
            });
        }

        /**
         * Returns a {@link Spliterator} over the elements of this reversed list, in reversed order.
         *
         * @return a {@code Spliterator} over the elements of this list in reversed order
         */
        @Override
        public Spliterator<E> spliterator() {
            // ImmutableCollection.spliterator() delegates to the backing collection, which for this view
            // is the FORWARD list; inheriting it would silently traverse the wrong order (and so would
            // every stream() built on it). Use the iterator-based spliterator, which goes through the
            // reversed iterator() above.
            return Spliterators.spliterator(this, Spliterator.ORDERED);
        }

        /**
         * Performs the given action for each element of this reversed list, in reversed order.
         *
         * @param action the action to be performed for each element
         * @throws NullPointerException if {@code action} is {@code null}
         */
        @Override
        public void forEach(final Consumer<? super E> action) throws NullPointerException {
            // Same reason as spliterator(): the inherited implementation would traverse the forward list.
            Objects.requireNonNull(action);

            iterator().forEachRemaining(action);
        }

        /**
         * Returns an array containing all elements of this reversed list in proper sequence.
         *
         * @return an {@code Object[]} containing all elements in reversed order
         */
        @Override
        public Object[] toArray() {
            final int size = size();
            final Object[] result = new Object[size];
            final Iterator<E> iter = iterator();

            for (int i = 0; i < size; i++) {
                result[i] = iter.next();
            }

            return result;
        }

        /**
         * Returns an array containing all elements of this reversed list in proper sequence.
         * The runtime type of the returned array is that of the specified array.
         * If the list fits in the specified array, it is returned therein; otherwise
         * a new array of the same runtime type is allocated.
         * If the specified array has room to spare, the element immediately following
         * the end of this list is set to {@code null}.
         *
         * @param <T> the component type of the array to contain the collection
         * @param a the array into which the elements are stored, if large enough; otherwise
         *        a new array of the same runtime type is allocated for this purpose
         * @return an array containing all elements of this reversed list
         * @throws NullPointerException if {@code a} is {@code null}
         * @throws ArrayStoreException if an element is incompatible with the runtime component type of {@code a}
         */
        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(final T[] a) throws NullPointerException, ArrayStoreException {
            final int size = size();
            final T[] result = a.length >= size ? a : (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
            final Iterator<E> iter = iterator();

            for (int i = 0; i < size; i++) {
                result[i] = (T) iter.next();
            }

            if (result.length > size) {
                result[size] = null;
            }

            return result;
        }

        /**
         * Compares the specified object with this reversed list for equality.
         * Returns {@code true} if the given object is also a {@link java.util.List} of the same size
         * and contains the same elements in the same order as this list's (reversed) iteration order.
         *
         * @param obj the object to be compared for equality with this list
         * @return {@code true} if the specified object is equal to this list
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof java.util.List<?> other)) {
                return false;
            }

            final int size = size();

            if (other.size() != size) {
                return false;
            }

            final Iterator<?> otherItr = other.iterator();
            final Iterator<E> iter = iterator();

            for (int i = 0; i < size; i++) {
                if (!N.equals(iter.next(), otherItr.next())) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Returns the hash code value for this reversed list, computed over its elements
         * in reversed order as specified by {@link java.util.List#hashCode()}.
         *
         * @return the hash code value for this list
         */
        @Override
        public int hashCode() {
            int hashCode = 1;

            for (final E element : this) {
                hashCode = 31 * hashCode + N.hashCode(element);
            }

            return hashCode;
        }

        /**
         * Marks this list as a reordered view so {@link ImmutableCollection#equals(Object)} does not
         * compare it via the (forward-ordered) backing collection.
         *
         * @return {@code true} always
         */
        @Override
        boolean isReorderedView() {
            return true;
        }

        /**
         * Returns a string representation of this reversed list in iteration (reversed) order.
         *
         * @return a string representation of this list
         */
        @Override
        public String toString() {
            return super.toString();
        }

        private static int reverseIndex(final int index, final int size) {
            return (size - 1) - index;
        }

        private static int reversePosition(final int index, final int size) {
            return size - index;
        }
    }

    /**
     * An {@link ImmutableList} whose backing list is {@link RandomAccess}, so this view is marked
     * {@code RandomAccess} too. Instances are produced by {@link ImmutableList#create(List, boolean, boolean)};
     * the class adds no state and no behaviour beyond the marker interface.
     *
     * @param <E> the type of elements in this list
     */
    @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
    static final class RandomAccessImmutableList<E> extends ImmutableList<E> implements RandomAccess {

        /**
         * Constructs a {@code RandomAccess} immutable list over the given backing list.
         *
         * @param list the backing list, which must itself be {@link RandomAccess}
         * @param isUnmodifiable {@code true} if {@code list} is already unmodifiable
         * @param ownsBacking see {@link ImmutableCollection#ownsBacking}
         * @throws NullPointerException if {@code list} is {@code null} and {@code isUnmodifiable} is false
         */
        RandomAccessImmutableList(final List<? extends E> list, final boolean isUnmodifiable, final boolean ownsBacking) throws NullPointerException {
            super(list, isUnmodifiable, ownsBacking);
        }
    }

    /**
     * Creates a new Builder for constructing an ImmutableList.
     * The builder allows adding elements one by one and then creating an immutable list.
     * This is useful when the number of elements is not known at compile time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> list = ImmutableList.<String>builder()
     *     .add("first")
     *     .add("second", "third")
     *     .addAll(Arrays.asList("fourth", "fifth"))
     *     .build();
     * }</pre>
     *
     * <p>The builder uses its own private storage, so the list returned by {@link Builder#build()} is an
     * independent, stable value once the builder has been consumed.</p>
     *
     * @param <E> the type of elements to be maintained by the list.
     * @return a new Builder instance for creating an ImmutableList.
     */
    public static <E> Builder<E> builder() {
        return new Builder<>(new ArrayList<>(), true);
    }

    /**
     * Creates a new Builder for constructing an ImmutableList using the provided list as storage.
     * The builder will add elements to the provided list and then create an immutable view of it.
     * This allows reusing an existing list instance as the backing storage.
     * Note that the provided list should not be modified outside the builder after this call.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> backingList = new ArrayList<>();
     * ImmutableList<Integer> numbers = ImmutableList.builder(backingList)
     *     .add(1)
     *     .add(2, 3, 4)
     *     .build();
     * }</pre>
     *
     * <p><b>Warning:</b> the caller keeps a reference to {@code holder}, so the list returned by
     * {@link Builder#build()} is a live view over storage the caller can still modify. It is treated as
     * such: {@link #copyOf(Collection)} will copy it rather than return it unchanged. Use the no-arg
     * {@link #builder()} when an independent value is wanted.</p>
     *
     * @param <E> the type of elements to be maintained by the list.
     * @param holder the list to be used as the backing storage for the Builder; must not be {@code null}.
     * @return a new Builder instance that will use the provided list.
     * @throws IllegalArgumentException if holder is {@code null}.
     */
    public static <E> Builder<E> builder(final List<E> holder) throws IllegalArgumentException {
        N.checkArgNotNull(holder, cs.holder);

        return new Builder<>(holder, false);
    }

    /**
     * A builder for creating ImmutableList instances.
     * The builder pattern allows for flexible construction of immutable lists,
     * especially useful when elements are added conditionally or in loops.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> moreElements = Arrays.asList("four", "five");
     * ImmutableList<String> list = ImmutableList.<String>builder()
     *     .add("one")
     *     .add("two", "three")
     *     .addAll(moreElements)
     *     .build();
     * }</pre>
     *
     * @param <E> the type of elements in the list being built.
     */
    public static final class Builder<E> {
        private final List<E> list;

        /** Whether {@link #list} is the builder's own storage, unreachable to any caller. */
        private final boolean ownsStorage;

        /** Set by {@link #build()}; further element additions are rejected from then on. */
        private boolean built;

        /**
         * Constructs a {@code Builder} that uses the given list as its backing storage.
         *
         * @param holder the list to accumulate elements into
         * @param ownsStorage {@code true} if {@code holder} was created by this class and no caller can reach it
         */
        Builder(final List<E> holder, final boolean ownsStorage) {
            list = holder;
            this.ownsStorage = ownsStorage;
        }

        /**
         * @throws IllegalStateException if this builder has already been consumed by {@code build()}
         */
        private void assertNotBuilt() throws IllegalStateException {
            if (built) {
                throw new IllegalStateException("This builder has already been consumed by build() and cannot be modified");
            }
        }

        /**
         * Adds a single element to the list being built.
         * The element is added to the end of the list.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.add("hello").add("world");
         * }</pre>
         *
         * @param element the element to add, may be {@code null}.
         * @return this builder instance for method chaining.
         * @throws IllegalStateException if {@link #build()} has already been called on this builder.
         */
        public Builder<E> add(final E element) throws IllegalStateException {
            assertNotBuilt();

            list.add(element);

            return this;
        }

        /**
         * Adds all provided elements to the list being built.
         * The elements are added to the end of the list in the order they appear in the array.
         * If the array is {@code null} or empty, no elements are added.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * builder.add("one", "two", "three");
         * }</pre>
         *
         * @param elements the elements to add, may be {@code null} or empty.
         * @return this builder instance for method chaining.
         * @throws IllegalStateException if {@link #build()} has already been called on this builder.
         */
        @SafeVarargs
        public final Builder<E> add(final E... elements) throws IllegalStateException {
            assertNotBuilt();

            if (N.notEmpty(elements)) {
                list.addAll(Arrays.asList(elements));
            }

            return this;
        }

        /**
         * Adds all elements from the specified collection to the list being built.
         * The elements are added to the end of the list in the order returned by
         * the collection's iterator. If the collection is {@code null} or empty, no elements are added.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> moreElements = Arrays.asList("four", "five");
         * builder.addAll(moreElements);
         * }</pre>
         *
         * @param c the collection containing elements to add, may be {@code null} or empty.
         * @return this builder instance for method chaining.
         * @throws IllegalStateException if {@link #build()} has already been called on this builder.
         */
        public Builder<E> addAll(final Collection<? extends E> c) throws IllegalStateException {
            assertNotBuilt();

            if (N.notEmpty(c)) {
                list.addAll(c);
            }

            return this;
        }

        /**
         * Adds all elements from the specified iterator to the list being built.
         * The elements are added to the end of the list in the order returned by the iterator.
         * The iterator is consumed by this operation. If the iterator is {@code null} or has no elements,
         * no elements are added.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> source = Arrays.asList("six", "seven");
         * Iterator<String> iter = source.iterator();
         * builder.addAll(iter);
         * }</pre>
         *
         * @param iter the iterator over elements to add, may be {@code null}.
         * @return this builder instance for method chaining.
         * @throws IllegalStateException if {@link #build()} has already been called on this builder.
         */
        public Builder<E> addAll(final Iterator<? extends E> iter) throws IllegalStateException {
            assertNotBuilt();

            if (iter != null) {
                while (iter.hasNext()) {
                    list.add(iter.next());
                }
            }

            return this;
        }

        /**
         * Builds and returns an ImmutableList containing all elements added to this builder.
         * The returned list is backed by the builder's storage rather than by a copy, so this method
         * consumes the builder: any subsequent {@code add}/{@code addAll} call throws
         * {@link IllegalStateException}. {@code build()} itself may be called more than once and
         * returns an equal list each time.
         *
         * <p>The returned list is immutable and will throw UnsupportedOperationException
         * for any modification attempts. When the builder was created by {@link ImmutableList#builder()}
         * its storage is private and the result is an independent value; when it was created by
         * {@link ImmutableList#builder(List)} the caller can still modify the holder it supplied, so the
         * result stays a live view and {@link ImmutableList#copyOf(Collection)} will copy it.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ImmutableList<String> finalList = builder.build();
         * System.out.println(finalList.size());   // prints the number of elements added
         * // builder.add("more");   // throws IllegalStateException
         * }</pre>
         *
         * @return a new ImmutableList containing all added elements in the order they were added.
         */
        public ImmutableList<E> build() {
            built = true;

            return create(list, false, ownsStorage);
        }
    }
}

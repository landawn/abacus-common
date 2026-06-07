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
import java.util.function.UnaryOperator;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * An immutable, thread-safe implementation of the {@link List} interface.
 * Once created, the contents of an ImmutableList cannot be modified.
 * All mutating operations (add, remove, set, sort, etc.) will throw {@link UnsupportedOperationException}.
 *
 * <p>This class provides several static factory methods for creating instances:
 * <ul>
 *   <li>{@link #empty()} - returns an empty list</li>
 *   <li>{@link #of(Object)} (and arity-overloads up to nine elements) - creates lists with specific elements</li>
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
 * ImmutableList<Integer> reversedView = ImmutableList.of(1, 2, 3).reversed();
 * // reversed contains [3, 2, 1]
 * }</pre>
 *
 * @param <E> the type of elements in this list
 * @see List
 * @see ImmutableCollection
 */
@SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
@SuppressWarnings("java:S2160")
public sealed class ImmutableList<E> extends ImmutableCollection<E> implements List<E> permits ImmutableList.ReverseImmutableList {

    @SuppressWarnings("rawtypes")
    private static final ImmutableList EMPTY = new ImmutableList(N.emptyList(), false);

    final List<E> list;

    /**
     * Constructs an ImmutableList backed by the provided list.
     * Whether the list is already unmodifiable is detected automatically.
     *
     * @param list the list of elements to be included in this ImmutableList.
     */
    ImmutableList(final List<? extends E> list) {
        this(list, ClassUtil.isPossibleImmutable(list.getClass()));
    }

    /**
     * Constructs an ImmutableList backed by the provided list.
     * If {@code isUnmodifiable} is {@code false}, the list is wrapped in an unmodifiable view.
     *
     * @param list the list of elements to be included in this ImmutableList.
     * @param isUnmodifiable {@code true} if the provided list is already unmodifiable and does not need wrapping.
     */
    @SuppressFBWarnings("BC_BAD_CAST_TO_ABSTRACT_COLLECTION")
    ImmutableList(final List<? extends E> list, final boolean isUnmodifiable) {
        super(isUnmodifiable ? list : Collections.unmodifiableList(list));
        this.list = (List<E>) coll;
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

    //    /**
    //     * Returns an ImmutableList containing a single element.
    //     * This is a convenience method equivalent to {@link #of(Object)}.
    //     *
    //     * <p><b>Usage Examples:</b></p>
    //     * <pre>{@code
    //     * ImmutableList<String> single = ImmutableList.just("hello");
    //     * System.out.println(single.get(0));   // prints: hello
    //     * }</pre>
    //     *
    //     * @param <E> the type of the element.
    //     * @param e the single element to be contained in the ImmutableList.
    //     * @return an ImmutableList containing only the specified element.
    //     * @deprecated
    //     */
    //    @Deprecated
    //    public static <E> ImmutableList<E> just(final E e) {
    //        return new ImmutableList<>(Array.asList(e), false);
    //    }

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
        return new ImmutableList<>(Array.asList(e1), false);
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
        return new ImmutableList<>(Array.asList(e1, e2), false);
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
        return new ImmutableList<>(Array.asList(e1, e2, e3), false);
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
        return new ImmutableList<>(Array.asList(e1, e2, e3, e4), false);
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
        return new ImmutableList<>(Array.asList(e1, e2, e3, e4, e5), false);
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
        return new ImmutableList<>(Array.asList(e1, e2, e3, e4, e5, e6), false);
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
        return new ImmutableList<>(Array.asList(e1, e2, e3, e4, e5, e6, e7), false);
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
        return new ImmutableList<>(Array.asList(e1, e2, e3, e4, e5, e6, e7, e8), false);
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
        return new ImmutableList<>(Array.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9), false);
    }

    //    /**
    //     * Returns an ImmutableList containing all elements from the provided array in the same order.
    //     * The returned list is independent of the input array; changes to the array after this call
    //     * will not affect the returned list. If the array is {@code null} or empty, an empty ImmutableList is returned.
    //     * Unlike some collection frameworks, this method supports {@code null} elements in the array.
    //     *
    //     * <p><b>Usage Examples:</b></p>
    //     * <pre>{@code
    //     * String[] array = {"one", "two", "three"};
    //     * ImmutableList<String> list = ImmutableList.of(array);
    //     * array[0] = "modified";  // Does not affect list
    //     * }</pre>
    //     *
    //     * @param <E> the type of the elements.
    //     * @param a the array of elements to include in the ImmutableList, may be {@code null} or empty.
    //     * @return an ImmutableList containing all elements from the array, or empty list if array is null/empty.
    //     * @see List#of(Object...)
    //     * @deprecated
    //     */
    //    @Deprecated
    //    @SafeVarargs
    //    public static <E> ImmutableList<E> of(final E... a) {
    //        if (N.isEmpty(a)) {
    //            return empty();
    //        } else {
    //            // return new ImmutableList<>(List.of(a), true);   // Doesn't support null element
    //            return new ImmutableList<>(Array.asList(a), false);
    //        }
    //    }

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
    public static <E> ImmutableList<E> copyOf(E[] elements) {
        if (N.isEmpty(elements)) {
            return empty();
        } else if (elements.length == 1) {
            return of(elements[0]);
        } else {
            return copyOf(N.toList(elements));
        }
    }

    /**
     * Returns an ImmutableList containing all elements from the provided collection.
     * If the provided collection is already an ImmutableList, it is returned directly without copying.
     * If the collection is {@code null} or empty, an empty ImmutableList is returned.
     * Otherwise, a new ImmutableList is created with a defensive copy of the collection's elements.
     * The order of elements is preserved as provided by the collection's iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> mutable = new ArrayList<>(Arrays.asList(1, 2, 3));
     * ImmutableList<Integer> immutable = ImmutableList.copyOf(mutable);
     * mutable.add(4);   // Does not affect immutable
     * }</pre>
     *
     * @param <E> the type of elements in the collection.
     * @param c the collection whose elements are to be placed into the {@code ImmutableList};
     *        may be {@code null} or empty.
     * @return the same instance if {@code c} is already an {@code ImmutableList};
     *         an empty {@code ImmutableList} if {@code c} is {@code null} or empty;
     *         otherwise a new {@code ImmutableList} containing a defensive copy of the collection's elements.
     * @see #copyOf(Object[])
     */
    public static <E> ImmutableList<E> copyOf(final Collection<? extends E> c) {
        if (c instanceof ImmutableList) {
            return (ImmutableList<E>) c;
        } else if (N.isEmpty(c)) {
            return empty();
        } else {
            return new ImmutableList<>(new ArrayList<>(c), false);
        }
    }

    /**
     * Wraps the provided list into an ImmutableList without copying the elements.
     * If the provided list is already an ImmutableList, it is returned directly.
     * If the list is {@code null}, an empty ImmutableList is returned.
     *
     * <p><b>Warning:</b> This method does not create a defensive copy. Changes to the
     * underlying List will be reflected in the returned ImmutableList, which
     * violates the immutability contract. For a {@code true} immutable copy, use
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
            return new ImmutableList<>(list);
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
    public E get(final int index) {
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
     * @param valueToFind the element to search for, may be null.
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
     * @param valueToFind the element to search for, may be null.
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
    public ImmutableListIterator<E> listIterator(final int index) {
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
     * @throws IndexOutOfBoundsException for an illegal endpoint index value
     *         (fromIndex &lt; 0 || toIndex &gt; size || fromIndex &gt; toIndex).
     * @see List#subList(int, int)
     */
    @Override
    public ImmutableList<E> subList(final int fromIndex, final int toIndex) {
        return ImmutableList.wrap(list.subList(fromIndex, toIndex));
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
     * @param operator ignored.
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
     * @param c ignored.
     * @throws UnsupportedOperationException always.
     * @deprecated ImmutableList does not support modification operations.
     */
    @Deprecated
    @Override
    public void sort(final Comparator<? super E> c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a view of this immutable list in reverse order. For example,
     * {@code ImmutableList.of(1, 2, 3).reversed()} returns a list containing {@code [3, 2, 1]}.
     * The returned list is backed by this list, so it's still immutable and reflects the
     * current state of this list. The reverse operation is efficient and does not copy elements.
     *
     * <p>If this list has one or zero elements, this same instance is returned.
     * Calling reversed() on an already reversed list returns the original list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> original = ImmutableList.of("a", "b", "c");
     * ImmutableList<String> reversed = original.reversed();
     * // reversed contains ["c", "b", "a"]
     *
     * ImmutableList<String> backToOriginal = reversed.reversed();
     * // backToOriginal is the same instance as original
     * }</pre>
     *
     * @return an immutable view of this list with elements in reverse order;
     *         this same instance if this list has zero or one element.
     */
    public ImmutableList<E> reversed() {
        return (size() <= 1) ? this : new ReverseImmutableList<>(this);
    }

    /**
     * An immutable list that presents the elements of a backing {@link ImmutableList} in reverse order.
     * Instances are created by {@link ImmutableList#reversed()} and are themselves immutable.
     * All mutating operations throw {@link UnsupportedOperationException}.
     *
     * @param <E> the type of elements in this list
     */
    @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
    static final class ReverseImmutableList<E> extends ImmutableList<E> {
        private final ImmutableList<E> forwardList;
        private final int size;

        /**
         * Constructs a reversed view of the given immutable list.
         *
         * @param backingList the list whose elements this view presents in reverse order
         */
        ReverseImmutableList(final ImmutableList<E> backingList) {
            super(backingList.list, true);
            forwardList = backingList;
            size = forwardList.size();
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

            return (index >= 0) ? reverseIndex(index) : -1;
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

            return (index >= 0) ? reverseIndex(index) : -1;
        }

        /**
         * Returns an immutable reversed view of the specified range of this list.
         * The returned sublist reflects the reversed ordering of elements in this list.
         *
         * @param fromIndex low endpoint (inclusive) of the sublist
         * @param toIndex high endpoint (exclusive) of the sublist
         * @return an immutable reversed view of the specified range
         * @throws IndexOutOfBoundsException if {@code fromIndex < 0 || toIndex > size() || fromIndex > toIndex}
         */
        @Override
        public ImmutableList<E> subList(final int fromIndex, final int toIndex) {
            N.checkFromToIndex(fromIndex, toIndex, size());

            return forwardList.subList(reversePosition(toIndex), reversePosition(fromIndex)).reversed();
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
        public E get(final int index) {
            return forwardList.get(reverseIndex(index));
        }

        /**
         * Returns the number of elements in this list.
         *
         * @return the number of elements
         */
        @Override
        public int size() {
            return size;
        }

        /**
         * Returns an iterator over the elements of this reversed list in reverse order
         * (i.e., from the last element of the forward list to the first).
         *
         * @return an {@link ObjIterator} over the elements in reverse order
         */
        @Override
        public ObjIterator<E> iterator() {
            return new ObjIterator<>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < size;
                }

                @Override
                public E next() {
                    if (!hasNext()) {
                        throw new java.util.NoSuchElementException();
                    }
                    return forwardList.get(reverseIndex(index++));
                }
            };
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
        public ImmutableListIterator<E> listIterator(final int index) {
            N.checkFromIndexSize(index, 0, size);

            return ImmutableListIterator.of(new java.util.ListIterator<>() {
                private int cursor = index;

                @Override
                public boolean hasNext() {
                    return cursor < size;
                }

                @Override
                public E next() {
                    if (!hasNext()) {
                        throw new java.util.NoSuchElementException();
                    }
                    return forwardList.get(reverseIndex(cursor++));
                }

                @Override
                public boolean hasPrevious() {
                    return cursor > 0;
                }

                @Override
                public E previous() {
                    if (!hasPrevious()) {
                        throw new java.util.NoSuchElementException();
                    }
                    return forwardList.get(reverseIndex(--cursor));
                }

                @Override
                public int nextIndex() {
                    return cursor;
                }

                @Override
                public int previousIndex() {
                    return cursor - 1;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(final E e) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void add(final E e) {
                    throw new UnsupportedOperationException();
                }
            });
        }

        /**
         * Returns an array containing all elements of this reversed list in proper sequence.
         *
         * @return an {@code Object[]} containing all elements in reversed order
         */
        @Override
        public Object[] toArray() {
            final Object[] result = new Object[size];
            for (int i = 0; i < size; i++) {
                result[i] = forwardList.get(reverseIndex(i));
            }
            return result;
        }

        /**
         * Returns an array containing all elements of this reversed list in proper sequence.
         * The runtime type of the returned array is that of the specified array.
         * If the list fits in the specified array, it is returned therein; otherwise
         * a new array of the same runtime type is allocated.
         *
         * @param <T> the component type of the array to contain the collection
         * @param a the array into which the elements are stored, if large enough; otherwise
         *        a new array of the same runtime type is allocated for this purpose
         * @return an array containing all elements of this reversed list
         * @throws ArrayStoreException if the runtime type of {@code a} is not a supertype of the
         *         runtime type of every element in this list
         * @throws NullPointerException if {@code a} is {@code null}
         */
        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(final T[] a) {
            final T[] result = a.length >= size ? a : (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
            for (int i = 0; i < size; i++) {
                result[i] = (T) forwardList.get(reverseIndex(i));
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

            if (other.size() != size) {
                return false;
            }

            final Iterator<?> otherItr = other.iterator();

            for (int i = 0; i < size; i++) {
                if (!N.equals(forwardList.get(reverseIndex(i)), otherItr.next())) {
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

            for (int i = 0; i < size; i++) {
                hashCode = 31 * hashCode + N.hashCode(forwardList.get(reverseIndex(i)));
            }

            return hashCode;
        }

        private int reverseIndex(final int index) {
            return (size - 1) - index;
        }

        private int reversePosition(final int index) {
            return size - index;
        }
    }

    /**
     * Creates a new Builder for constructing an ImmutableList.
     * The builder allows adding elements one by one and then creating an immutable list.
     * This is useful when the number of elements is not known at compile time.
     * The builder uses an ArrayList internally for efficient element addition.
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
     * @param <E> the type of elements to be maintained by the list.
     * @return a new Builder instance for creating an ImmutableList.
     */
    public static <E> Builder<E> builder() {
        return new Builder<>(new ArrayList<>());
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
     * @param <E> the type of elements to be maintained by the list.
     * @param holder the list to be used as the backing storage for the Builder.
     * @return a new Builder instance that will use the provided list.
     */
    public static <E> Builder<E> builder(final List<E> holder) {
        return new Builder<>(holder);
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

        /**
         * Constructs a {@code Builder} that uses the given list as its backing storage.
         *
         * @param holder the list to accumulate elements into
         */
        Builder(final List<E> holder) {
            list = holder;
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
         * @param element the element to add, may be null.
         * @return this builder instance for method chaining.
         */
        public Builder<E> add(final E element) {
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
         */
        @SafeVarargs
        public final Builder<E> add(final E... elements) {
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
         */
        public Builder<E> addAll(final Collection<? extends E> c) {
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
         * Iterator<String> iter = someCollection.iterator();
         * builder.addAll(iter);
         * }</pre>
         *
         * @param iter the iterator over elements to add, may be null.
         * @return this builder instance for method chaining.
         */
        public Builder<E> addAll(final Iterator<? extends E> iter) {
            if (iter != null) {
                while (iter.hasNext()) {
                    list.add(iter.next());
                }
            }

            return this;
        }

        /**
         * Builds and returns an ImmutableList containing all elements added to this builder.
         * After calling this method, the builder should not be used further as the created
         * ImmutableList may be backed by the builder's internal storage.
         *
         * <p>The returned list is immutable and will throw UnsupportedOperationException
         * for any modification attempts.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ImmutableList<String> finalList = builder.build();
         * System.out.println(finalList.size());   // prints the number of elements added
         * }</pre>
         *
         * @return a new ImmutableList containing all added elements in the order they were added.
         */
        public ImmutableList<E> build() {
            return new ImmutableList<>(list);
        }
    }
}

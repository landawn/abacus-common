/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 *
 * @param <E>
 */
@SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
@SuppressWarnings("java:S2160")
public sealed class ImmutableList<E> extends ImmutableCollection<E> implements List<E> permits ImmutableList.ReverseImmutableList {

    @SuppressWarnings("rawtypes")
    private static final ImmutableList EMPTY = new ImmutableList(N.emptyList(), false);

    final List<E> list;

    /**
     * Constructs an ImmutableList instance with the provided list.
     * The list is not made unmodifiable in this constructor, it's handled in another constructor.
     *
     * @param list the list of elements to be included in this ImmutableList
     */
    ImmutableList(final List<? extends E> list) {
        this(list, ClassUtil.isPossibleImmutable(list.getClass()));
    }

    /**
     * Constructs an ImmutableList instance with the provided list and a boolean indicating if the list is unmodifiable.
     * If the list is not unmodifiable, it is wrapped into an unmodifiable list.
     *
     * @param list the list of elements to be included in this ImmutableList
     * @param isUnmodifiable a boolean indicating if the provided list is unmodifiable
     */
    @SuppressFBWarnings("BC_BAD_CAST_TO_ABSTRACT_COLLECTION")
    ImmutableList(final List<? extends E> list, final boolean isUnmodifiable) {
        super(isUnmodifiable ? list : Collections.unmodifiableList(list));
        this.list = (List<E>) coll;
    }

    /**
     * Returns an empty ImmutableList.
     *
     * @param <E> the type of elements in this list
     * @return an empty ImmutableList
     */
    public static <E> ImmutableList<E> empty() {
        return EMPTY;
    }

    /**
     * Returns an ImmutableList containing just the provided element.
     *
     * @param <E> the type of the element
     * @param e the element to be included in the ImmutableList
     * @return an ImmutableList containing the provided element
     */
    public static <E> ImmutableList<E> just(final E e) {
        return new ImmutableList<>(Array.asList(e), false);
    }

    /**
     * Returns an ImmutableList containing just the provided element.
     *
     * @param <E> the type of the element
     * @param e1 the element to be included in the ImmutableList
     * @return an ImmutableList containing the provided element
     */
    public static <E> ImmutableList<E> of(final E e1) {
        return new ImmutableList<>(Array.asList(e1), false);
    }

    /**
     * Returns an ImmutableList containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableList
     * @param e2 the second element to be included in the ImmutableList
     * @return an ImmutableList containing the provided elements
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2) {
        return new ImmutableList<>(Array.asList(e1, e2), false);
    }

    /**
     * Returns an ImmutableList containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableList
     * @param e2 the second element to be included in the ImmutableList
     * @param e3 the third element to be included in the ImmutableList
     * @return an ImmutableList containing the provided elements
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3) {
        return new ImmutableList<>(Array.asList(e1, e2, e3), false);
    }

    /**
     * Returns an ImmutableList containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableList
     * @param e2 the second element to be included in the ImmutableList
     * @param e3 the third element to be included in the ImmutableList
     * @param e4 the fourth element to be included in the ImmutableList
     * @return an ImmutableList containing the provided elements
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4) {
        return new ImmutableList<>(Array.asList(e1, e2, e3, e4), false);
    }

    /**
     * Returns an ImmutableList containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableList
     * @param e2 the second element to be included in the ImmutableList
     * @param e3 the third element to be included in the ImmutableList
     * @param e4 the fourth element to be included in the ImmutableList
     * @param e5 the fifth element to be included in the ImmutableList
     * @return an ImmutableList containing the provided elements
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5) {
        return new ImmutableList<>(Array.asList(e1, e2, e3, e4, e5), false);
    }

    /**
     * Returns an ImmutableList containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableList
     * @param e2 the second element to be included in the ImmutableList
     * @param e3 the third element to be included in the ImmutableList
     * @param e4 the fourth element to be included in the ImmutableList
     * @param e5 the fifth element to be included in the ImmutableList
     * @param e6 the sixth element to be included in the ImmutableList
     * @return an ImmutableList containing the provided elements
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6) {
        return new ImmutableList<>(Array.asList(e1, e2, e3, e4, e5, e6), false);
    }

    /**
     * Returns an ImmutableList containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableList
     * @param e2 the second element to be included in the ImmutableList
     * @param e3 the third element to be included in the ImmutableList
     * @param e4 the fourth element to be included in the ImmutableList
     * @param e5 the fifth element to be included in the ImmutableList
     * @param e6 the sixth element to be included in the ImmutableList
     * @param e7 the seventh element to be included in the ImmutableList
     * @return an ImmutableList containing the provided elements
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7) {
        return new ImmutableList<>(Array.asList(e1, e2, e3, e4, e5, e6, e7), false);
    }

    /**
     * Returns an ImmutableList containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableList
     * @param e2 the second element to be included in the ImmutableList
     * @param e3 the third element to be included in the ImmutableList
     * @param e4 the fourth element to be included in the ImmutableList
     * @param e5 the fifth element to be included in the ImmutableList
     * @param e6 the sixth element to be included in the ImmutableList
     * @param e7 the seventh element to be included in the ImmutableList
     * @param e8 the eighth element to be included in the ImmutableList
     * @return an ImmutableList containing the provided elements
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8) {
        return new ImmutableList<>(Array.asList(e1, e2, e3, e4, e5, e6, e7, e8), false);
    }

    /**
     * Returns an ImmutableList containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableList
     * @param e2 the second element to be included in the ImmutableList
     * @param e3 the third element to be included in the ImmutableList
     * @param e4 the fourth element to be included in the ImmutableList
     * @param e5 the fifth element to be included in the ImmutableList
     * @param e6 the sixth element to be included in the ImmutableList
     * @param e7 the seventh element to be included in the ImmutableList
     * @param e8 the eighth element to be included in the ImmutableList
     * @param e9 the ninth element to be included in the ImmutableList
     * @return an ImmutableList containing the provided elements
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8, final E e9) {
        return new ImmutableList<>(Array.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9), false);
    }

    /**
     * Returns an ImmutableList containing the provided elements.
     *
     * @param <E> the type of the elements
     * @param e1 the first element to be included in the ImmutableList
     * @param e2 the second element to be included in the ImmutableList
     * @param e3 the third element to be included in the ImmutableList
     * @param e4 the fourth element to be included in the ImmutableList
     * @param e5 the fifth element to be included in the ImmutableList
     * @param e6 the sixth element to be included in the ImmutableList
     * @param e7 the seventh element to be included in the ImmutableList
     * @param e8 the eighth element to be included in the ImmutableList
     * @param e9 the ninth element to be included in the ImmutableList
     * @param e10 the tenth element to be included in the ImmutableList
     * @return an ImmutableList containing the provided elements
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8, final E e9,
            final E e10) {
        return new ImmutableList<>(Array.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10), false);
    }

    /**
     * Returns an ImmutableList containing the provided elements. And it's not backed by the specified array.
     * If the specified array is {@code null} or empty, an empty {@code ImmutableList} is returned.
     *
     * @param <E> the type of the elements
     * @param a the elements to be included in the ImmutableList
     * @return an ImmutableList containing the provided elements
     * @see List#of(Object...)
     */
    @SafeVarargs
    public static <E> ImmutableList<E> of(final E... a) {
        if (N.isEmpty(a)) {
            return empty();
        } else {
            return new ImmutableList<>(Array.asList(a), false);
        }
    }

    /**
     * Returns an ImmutableList containing the elements of the provided collection.
     * If the provided collection is already an instance of ImmutableList, it is directly returned.
     * If the provided collection is {@code null} or empty, an empty ImmutableList is returned.
     * Otherwise, a new ImmutableList is created with the elements of the provided collection.
     *
     * @param <E> the type of elements in the collection
     * @param c the collection whose elements are to be placed into this list
     * @return an ImmutableList containing the elements of the specified collection
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
     * Wraps the provided list into an ImmutableList. Changes to the specified List will be reflected in the ImmutableList.
     * If the provided list is already an instance of ImmutableList, it is directly returned.
     * If the list is {@code null}, an empty ImmutableList is returned.
     * Otherwise, returned a new ImmutableList backed by the provided list.
     *
     * @param <E> the type of elements in the list
     * @param list the list to be wrapped into an ImmutableList
     * @return an ImmutableList backed by the provided list
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
     * This method is deprecated and will throw an UnsupportedOperationException if used.
     *
     * @param <E>
     * @param c
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    public static <E> ImmutableCollection<E> wrap(final Collection<? extends E> c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index the index of the element to return
     * @return the element at the specified position in this list
     * @see List#get(int)
     */
    @Override
    public E get(final int index) {
        return list.get(index);
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * or -1 if this list does not contain the element.
     *
     * @param valueToFind the element to search for
     * @return the index of the first occurrence of the specified element in this list,
     *         or -1 if this list does not contain the element
     *
     * @see List#indexOf(Object)
     */
    @Override
    public int indexOf(final Object valueToFind) {
        return list.indexOf(valueToFind);
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * or -1 if this list does not contain the element.
     *
     * @param valueToFind the element to search for
     * @return the index of the last occurrence of the specified element in this list,
     *         or -1 if this list does not contain the element
     *
     * @see List#lastIndexOf(Object)
     */
    @Override
    public int lastIndexOf(final Object valueToFind) {
        return list.lastIndexOf(valueToFind);
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * @return an iterator over the elements in this list in proper sequence
     * @see List#iterator()
     */
    @Override
    public ImmutableListIterator<E> listIterator() {
        return ImmutableListIterator.of(list.listIterator());
    }

    /**
     * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified position in the list.
     * The specified index indicates the first element that would be returned by an initial call to {@code next}.
     * An initial call to {@code previous} would return the element with the specified index minus one.
     *
     * @param index index of the first element to be returned from the list iterator (by a call to {@code next})
     * @return a list iterator over the elements in this list (in proper sequence), starting at the specified position in the list
     * @see List#listIterator(int)
     */
    @Override
    public ImmutableListIterator<E> listIterator(final int index) {
        return ImmutableListIterator.of(list.listIterator(index));
    }

    /**
     * Returns a view of the portion of this list between the specified fromIndex, inclusive, and toIndex, exclusive.
     * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list.
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex high endpoint (exclusive) of the subList
     * @return a view of the specified range within this list
     * @see List#subList(int, int)
     */
    @Override
    public ImmutableList<E> subList(final int fromIndex, final int toIndex) {
        return ImmutableList.wrap(list.subList(fromIndex, toIndex));
    }

    /**
     * Adds the all.
     *
     * @param index
     * @param newElements
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public boolean addAll(final int index, final Collection<? extends E> newElements) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param index
     * @param element
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public E set(final int index, final E element) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param index
     * @param element
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public void add(final int index, final E element) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param index
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public E remove(final int index) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param operator
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public void replaceAll(final UnaryOperator<E> operator) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param c
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public void sort(final Comparator<? super E> c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * <p>Copied from Google Guava under Apache License v2.0 and may be modified.</p>
     *
     *
     * Returns a view of this immutable list in reverse order. For example, {@code ImmutableList.of(1, 2, 3).reverse()} is equivalent to {@code ImmutableList.of(3, 2, 1)}.
     *
     * @return a view of this immutable list in reverse order
     */
    public ImmutableList<E> reverse() {
        return (size() <= 1) ? this : new ReverseImmutableList<>(this);
    }

    @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
    static final class ReverseImmutableList<E> extends ImmutableList<E> {
        private final ImmutableList<E> forwardList;
        private final int size;

        ReverseImmutableList(final ImmutableList<E> backingList) {
            super(backingList.list, true);
            forwardList = backingList;
            size = forwardList.size();
        }

        @Override
        public ImmutableList<E> reverse() {
            return forwardList;
        }

        @Override
        public boolean contains(final Object object) {
            return forwardList.contains(object);
        }

        @Override
        public int indexOf(final Object object) {
            @SuppressWarnings("SuspiciousMethodCalls")
            final int index = forwardList.lastIndexOf(object);

            return (index >= 0) ? reverseIndex(index) : -1;
        }

        @Override
        public int lastIndexOf(final Object object) {
            @SuppressWarnings("SuspiciousMethodCalls")
            final int index = forwardList.indexOf(object);

            return (index >= 0) ? reverseIndex(index) : -1;
        }

        @Override
        public ImmutableList<E> subList(final int fromIndex, final int toIndex) {
            N.checkFromToIndex(fromIndex, toIndex, size());

            return forwardList.subList(reversePosition(toIndex), reversePosition(fromIndex)).reverse();
        }

        @Override
        public E get(final int index) {
            return forwardList.get(reverseIndex(index));
        }

        @Override
        public int size() {
            return size;
        }

        private int reverseIndex(final int index) {
            return (size - 1) - index;
        }

        private int reversePosition(final int index) {
            return size - index;
        }
    }

    /**
     * Creates a new Builder instance for constructing an ImmutableList.
     *
     * @param <E> the type of elements maintained by the list
     * @return a new Builder instance
     */
    public static <E> Builder<E> builder() {
        return new Builder<>(new ArrayList<>());
    }

    /**
     * Creates a new Builder instance for constructing an ImmutableList with the provided list as the holder.
     *
     * @param <E> the type of elements maintained by the list
     * @param holder the list to be used as the holder for the Builder
     * @return a new Builder instance
     */
    public static <E> Builder<E> builder(final List<E> holder) {
        return new Builder<>(holder);
    }

    public static final class Builder<E> {
        private final List<E> list;

        Builder(final List<E> holder) {
            list = holder;
        }

        /**
         *
         * @param element
         * @return
         */
        public Builder<E> add(final E element) {
            list.add(element);

            return this;
        }

        /**
         *
         * @param elements
         * @return
         */
        @SafeVarargs
        public final Builder<E> add(final E... elements) {
            if (N.notEmpty(elements)) {
                list.addAll(Arrays.asList(elements));
            }

            return this;
        }

        /**
         *
         * @param c
         * @return
         */
        public Builder<E> addAll(final Collection<? extends E> c) {
            if (N.notEmpty(c)) {
                list.addAll(c);
            }

            return this;
        }

        /**
         *
         * @param iter
         * @return
         */
        public Builder<E> addAll(final Iterator<? extends E> iter) {
            if (iter != null) {
                while (iter.hasNext()) {
                    list.add(iter.next());
                }
            }

            return this;
        }

        public ImmutableList<E> build() {
            return new ImmutableList<>(list);
        }
    }
}

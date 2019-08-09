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
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableList.
 *
 * @author Haiyang Li
 * @param <E> the element type
 * @since 0.8
 */
public final class ImmutableList<E> extends ImmutableCollection<E> implements List<E> {

    /** The Constant EMPTY. */
    @SuppressWarnings("rawtypes")
    private static final ImmutableList EMPTY = new ImmutableList(Collections.EMPTY_LIST);

    /** The list. */
    private final List<E> list;

    /**
     * Instantiates a new immutable list.
     *
     * @param list the list
     */
    ImmutableList(List<? extends E> list) {
        super(Collections.unmodifiableList(list));
        this.list = (List<E>) coll;
    }

    /**
     * Empty.
     *
     * @param <E> the element type
     * @return the immutable list
     */
    public static <E> ImmutableList<E> empty() {
        return EMPTY;
    }

    /**
     * Just.
     *
     * @param <E> the element type
     * @param e the e
     * @return the immutable list
     */
    public static <E> ImmutableList<E> just(E e) {
        return new ImmutableList<>(Collections.singletonList(e));
    }

    /**
     * Of.
     *
     * @param <E> the element type
     * @param e the e
     * @return the immutable list
     */
    public static <E> ImmutableList<E> of(E e) {
        return new ImmutableList<>(Collections.singletonList(e));
    }

    /**
     * Of.
     *
     * @param <E> the element type
     * @param a the elements in this <code>array</code> are shared by the returned ImmutableList.
     * @return the immutable list
     */
    @SafeVarargs
    public static <E> ImmutableList<E> of(E... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return new ImmutableList<>(Arrays.asList(a));
    }

    /**
     * Of.
     *
     * @param <E> the element type
     * @param list the elements in this <code>list</code> are shared by the returned ImmutableList.
     * @return the immutable list
     */
    public static <E> ImmutableList<E> of(final List<? extends E> list) {
        if (list == null) {
            return empty();
        } else if (list instanceof ImmutableList) {
            return (ImmutableList<E>) list;
        }

        return new ImmutableList<>(list);
    }

    /**
     * Copy of.
     *
     * @param <E> the element type
     * @param a the a
     * @return the immutable list
     */
    @SafeVarargs
    public static <E> ImmutableList<E> copyOf(final E... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return new ImmutableList<>(Arrays.asList(N.clone(a)));
    }

    /**
     * Copy of.
     *
     * @param <E> the element type
     * @param list the list
     * @return the immutable list
     */
    public static <E> ImmutableList<E> copyOf(final Collection<? extends E> list) {
        if (N.isNullOrEmpty(list)) {
            return empty();
        }

        return new ImmutableList<>(new ArrayList<>(list));
    }

    /**
     * Gets the.
     *
     * @param index the index
     * @return the e
     */
    @Override
    public E get(int index) {
        return list.get(index);
    }

    /**
     * Index of.
     *
     * @param o the o
     * @return the int
     */
    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    /**
     * Last index of.
     *
     * @param o the o
     * @return the int
     */
    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    /**
     * List iterator.
     *
     * @return the list iterator
     */
    @Override
    public ListIterator<E> listIterator() {
        return list.listIterator();
    }

    /**
     * List iterator.
     *
     * @param index the index
     * @return the list iterator
     */
    @Override
    public ListIterator<E> listIterator(int index) {
        return list.listIterator(index);
    }

    /**
     * Sub list.
     *
     * @param fromIndex the from index
     * @param toIndex the to index
     * @return the list
     */
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    /**
     * Adds the all.
     *
     * @param index the index
     * @param newElements the new elements
     * @return true, if successful
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final boolean addAll(int index, Collection<? extends E> newElements) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the.
     *
     * @param index the index
     * @param element the element
     * @return the e
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds the.
     *
     * @param index the index
     * @param element the element
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the.
     *
     * @param index the index
     * @return the e
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final E remove(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * Replace all.
     *
     * @param operator the operator
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final void replaceAll(UnaryOperator<E> operator) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sort.
     *
     * @param c the c
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final void sort(Comparator<? super E> c) {
        throw new UnsupportedOperationException();
    }
}

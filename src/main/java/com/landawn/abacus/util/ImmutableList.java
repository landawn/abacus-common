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
 * @param <E>
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
     * @param list
     */
    ImmutableList(List<? extends E> list) {
        super(Collections.unmodifiableList(list));
        this.list = (List<E>) coll;
    }

    /**
     *
     * @param <E>
     * @return
     */
    public static <E> ImmutableList<E> empty() {
        return EMPTY;
    }

    /**
     *
     * @param <E>
     * @param e
     * @return
     */
    public static <E> ImmutableList<E> just(E e) {
        return new ImmutableList<>(Collections.singletonList(e));
    }

    /**
     *
     * @param <E>
     * @param e
     * @return
     */
    public static <E> ImmutableList<E> of(E e) {
        return new ImmutableList<>(Collections.singletonList(e));
    }

    /**
     *
     * @param <E>
     * @param a the elements in this <code>array</code> are shared by the returned ImmutableList.
     * @return
     */
    @SafeVarargs
    public static <E> ImmutableList<E> of(E... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return new ImmutableList<>(Arrays.asList(a));
    }

    /**
     *
     * @param <E>
     * @param list the elements in this <code>list</code> are shared by the returned ImmutableList.
     * @return
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
     *
     * @param <E>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <E> ImmutableList<E> copyOf(final E... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return new ImmutableList<>(Arrays.asList(N.clone(a)));
    }

    /**
     *
     * @param <E>
     * @param list
     * @return
     */
    public static <E> ImmutableList<E> copyOf(final Collection<? extends E> list) {
        if (N.isNullOrEmpty(list)) {
            return empty();
        }

        return new ImmutableList<>(new ArrayList<>(list));
    }

    /**
     * 
     * @param <E>
     * @param c
     * @return
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    public static <E> ImmutableCollection<E> of(final Collection<? extends E> c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param index
     * @return
     */
    @Override
    public E get(int index) {
        return list.get(index);
    }

    /**
     *
     * @param o
     * @return
     */
    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    /**
     * Last index of.
     *
     * @param o
     * @return
     */
    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    /**
     *
     * @return
     */
    @Override
    public ListIterator<E> listIterator() {
        return list.listIterator();
    }

    /**
     *
     * @param index
     * @return
     */
    @Override
    public ListIterator<E> listIterator(int index) {
        return list.listIterator(index);
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    /**
     * Adds the all.
     *
     * @param index
     * @param newElements
     * @return true, if successful
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    @Override
    public final boolean addAll(int index, Collection<? extends E> newElements) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param index
     * @param element
     * @return
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    @Override
    public final E set(int index, E element) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param index
     * @param element
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    @Override
    public final void add(int index, E element) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param index
     * @return
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    @Override
    public final E remove(int index) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param operator
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    @Override
    public final void replaceAll(UnaryOperator<E> operator) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param c
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    @Override
    public final void sort(Comparator<? super E> c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}

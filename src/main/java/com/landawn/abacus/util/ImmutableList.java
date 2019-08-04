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

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class ImmutableList<E> extends ImmutableCollection<E> implements List<E> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableList EMPTY = new ImmutableList(Collections.EMPTY_LIST);

    private final List<E> list;

    ImmutableList(List<? extends E> list) {
        super(Collections.unmodifiableList(list));
        this.list = (List<E>) coll;
    }

    public static <E> ImmutableList<E> empty() {
        return EMPTY;
    }

    public static <E> ImmutableList<E> just(E e) {
        return new ImmutableList<>(Collections.singletonList(e));
    }

    public static <E> ImmutableList<E> of(E e) {
        return new ImmutableList<>(Collections.singletonList(e));
    }

    /**
     * 
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

    @SafeVarargs
    public static <E> ImmutableList<E> copyOf(final E... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return new ImmutableList<>(Arrays.asList(N.clone(a)));
    }

    public static <E> ImmutableList<E> copyOf(final Collection<? extends E> list) {
        if (N.isNullOrEmpty(list)) {
            return empty();
        }

        return new ImmutableList<>(new ArrayList<>(list));
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return list.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return list.listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    /**
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final boolean addAll(int index, Collection<? extends E> newElements) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final E remove(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final void replaceAll(UnaryOperator<E> operator) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public final void sort(Comparator<? super E> c) {
        throw new UnsupportedOperationException();
    }
}

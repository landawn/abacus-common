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

/**
 *
 * @author Haiyang Li
 * @param <E>
 * @since 0.8
 */
@SuppressWarnings("java:S2160")
public final class ImmutableList<E> extends ImmutableCollection<E> implements List<E> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableList EMPTY = new ImmutableList(Collections.emptyList());

    private final List<E> list;

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
     * @param <T>
     * @param e
     * @return
     */
    public static <T> ImmutableList<T> just(T e) {
        return new ImmutableList<>(Collections.singletonList(e));
    }

    /**
     * 
     *
     * @param <T> 
     * @param e1 
     * @return 
     */
    public static <T> ImmutableList<T> of(final T e1) {
        return new ImmutableList<>(N.asList(e1));
    }

    /**
     * 
     *
     * @param <T> 
     * @param e1 
     * @param e2 
     * @return 
     */
    public static <T> ImmutableList<T> of(final T e1, final T e2) {
        return new ImmutableList<>(N.asList(e1, e2));
    }

    /**
     * 
     *
     * @param <T> 
     * @param e1 
     * @param e2 
     * @param e3 
     * @return 
     */
    public static <T> ImmutableList<T> of(final T e1, final T e2, final T e3) {
        return new ImmutableList<>(N.asList(e1, e2, e3));
    }

    /**
     * 
     *
     * @param <T> 
     * @param e1 
     * @param e2 
     * @param e3 
     * @param e4 
     * @return 
     */
    public static <T> ImmutableList<T> of(final T e1, final T e2, final T e3, final T e4) {
        return new ImmutableList<>(N.asList(e1, e2, e3, e4));
    }

    /**
     * 
     *
     * @param <T> 
     * @param e1 
     * @param e2 
     * @param e3 
     * @param e4 
     * @param e5 
     * @return 
     */
    public static <T> ImmutableList<T> of(final T e1, final T e2, final T e3, final T e4, final T e5) {
        return new ImmutableList<>(N.asList(e1, e2, e3, e4, e5));
    }

    /**
     * 
     *
     * @param <T> 
     * @param e1 
     * @param e2 
     * @param e3 
     * @param e4 
     * @param e5 
     * @param e6 
     * @return 
     */
    public static <T> ImmutableList<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) {
        return new ImmutableList<>(N.asList(e1, e2, e3, e4, e5, e6));
    }

    /**
     * 
     *
     * @param <T> 
     * @param e1 
     * @param e2 
     * @param e3 
     * @param e4 
     * @param e5 
     * @param e6 
     * @param e7 
     * @return 
     */
    public static <T> ImmutableList<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7) {
        return new ImmutableList<>(N.asList(e1, e2, e3, e4, e5, e6, e7));
    }

    /**
     *
     * @param <E>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <E> ImmutableList<E> of(final E... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return new ImmutableList<>(Collections.singletonList(a[0]));
        } else {
            return new ImmutableList<>(N.asList(N.clone(a)));
        }
    }

    /**
     *
     * @param <E>
     * @param c
     * @return
     */
    public static <E> ImmutableList<E> copyOf(final Collection<? extends E> c) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return new ImmutableList<>(Collections.singletonList(N.firstOrNullIfEmpty(c)));
        } else {
            return new ImmutableList<>(new ArrayList<>(c));
        }
    }

    /**
     *
     * @param <E>
     * @param list
     * @return an {@code ImmutableList} backed by the specified {@code list}
     * @deprecated the ImmutableList may be modified through the specified {@code list}
     */
    @Deprecated
    public static <E> ImmutableList<E> wrap(final List<? extends E> list) {
        if (list == null) {
            return empty();
        } else if (list instanceof ImmutableList) {
            return (ImmutableList<E>) list;
        }

        return new ImmutableList<>(list);
    }

    /**
     * 
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
     *
     * @return 
     */
    @Override
    public ImmutableListIterator<E> listIterator() {
        return ImmutableListIterator.of(list.listIterator());
    }

    /**
     *
     * @param index
     * @return
     */
    @Override
    public ImmutableListIterator<E> listIterator(int index) {
        return ImmutableListIterator.of(list.listIterator(index));
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @return
     */
    @Override
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
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
    public boolean addAll(int index, Collection<? extends E> newElements) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @param index 
     * @param element 
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public E set(int index, E element) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @param index 
     * @param element 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public void add(int index, E element) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @param index 
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public E remove(int index) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @param operator 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public void replaceAll(UnaryOperator<E> operator) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @param c 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    @Override
    public void sort(Comparator<? super E> c) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @param <E> 
     * @return 
     */
    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    public static final class Builder<E> {
        private final List<E> ret = new ArrayList<>();

        /**
         * 
         *
         * @param element 
         * @return 
         */
        public Builder<E> add(final E element) {
            ret.add(element);

            return this;
        }

        /**
         * 
         *
         * @param elements 
         * @return 
         */
        public Builder<E> add(final E... elements) {
            if (N.notNullOrEmpty(elements)) {
                ret.addAll(Arrays.asList(elements));
            }

            return this;
        }

        /**
         * 
         *
         * @param c 
         * @return 
         */
        public Builder<E> addAll(final Collection<? extends E> c) {
            if (N.notNullOrEmpty(c)) {
                ret.addAll(c);
            }

            return this;
        }

        /**
         * 
         *
         * @param iter 
         * @return 
         */
        public Builder<E> addAll(final Iterator<? extends E> iter) {
            if (iter != null) {
                while (iter.hasNext()) {
                    ret.add(iter.next());
                }
            }

            return this;
        }

        /**
         * 
         *
         * @return 
         */
        public ImmutableList<E> build() {
            return new ImmutableList<>(ret);
        }
    }
}

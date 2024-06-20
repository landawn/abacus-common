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
    private static final ImmutableList EMPTY = new ImmutableList(List.of(), true);

    private final List<E> list;

    ImmutableList(final List<? extends E> list) {
        this(list, false);
    }

    ImmutableList(final List<? extends E> list, final boolean isUnmodifiable) {
        super(isUnmodifiable ? list : Collections.unmodifiableList(list));
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
        return new ImmutableList<>(List.of(e), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @return
     */
    public static <E> ImmutableList<E> of(final E e1) {
        return new ImmutableList<>(List.of(e1), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @return
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2) {
        return new ImmutableList<>(List.of(e1, e2), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @param e3
     * @return
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3) {
        return new ImmutableList<>(List.of(e1, e2, e3), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @return
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4) {
        return new ImmutableList<>(List.of(e1, e2, e3, e4), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @return
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5) {
        return new ImmutableList<>(List.of(e1, e2, e3, e4, e5), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @return
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6) {
        return new ImmutableList<>(List.of(e1, e2, e3, e4, e5, e6), true);
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @return
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7) {
        return new ImmutableList<>(List.of(e1, e2, e3, e4, e5, e6, e7), true);
    }

    /**
     * 
     *
     * @param <E> 
     * @param e1 
     * @param e2 
     * @param e3 
     * @param e4 
     * @param e5 
     * @param e6 
     * @param e7 
     * @param e8 
     * @return 
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8) {
        return new ImmutableList<>(List.of(e1, e2, e3, e4, e5, e6, e7, e8), true);
    }

    /**
     * 
     *
     * @param <E> 
     * @param e1 
     * @param e2 
     * @param e3 
     * @param e4 
     * @param e5 
     * @param e6 
     * @param e7 
     * @param e8 
     * @param e9 
     * @return 
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8, final E e9) {
        return new ImmutableList<>(List.of(e1, e2, e3, e4, e5, e6, e7, e8, e9), true);
    }

    /**
     * 
     *
     * @param <E> 
     * @param e1 
     * @param e2 
     * @param e3 
     * @param e4 
     * @param e5 
     * @param e6 
     * @param e7 
     * @param e8 
     * @param e9 
     * @param e10 
     * @return 
     */
    public static <E> ImmutableList<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7, final E e8, final E e9,
            final E e10) {
        return new ImmutableList<>(List.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10), true);
    }

    /**
     *
     * @param <E>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <E> ImmutableList<E> of(final E... a) {
        if (N.isEmpty(a)) {
            return empty();
        } else {
            return new ImmutableList<>(List.of(a), true);
        }
    }

    /**
     *
     * @param <E>
     * @param c
     * @return
     */
    public static <E> ImmutableList<E> copyOf(final Collection<? extends E> c) {
        if (c instanceof ImmutableList) {
            return (ImmutableList<E>) c;
        } else if (N.isEmpty(c)) {
            return empty();
        } else {
            return new ImmutableList<>(List.copyOf(c), true);
        }
    }

    /**
     *
     * @param <E>
     * @param list
     * @return an {@code ImmutableList} backed by the specified {@code list}
     */
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
        return new Builder<>(new ArrayList<>());
    }

    /**
     *
     *
     * @param <E>
     * @param holder
     * @return
     */
    public static <E> Builder<E> builder(final List<E> holder) {
        return new Builder<>(holder);
    }

    public static final class Builder<E> {
        private final List<E> list;

        Builder(final List<E> holder) {
            this.list = holder;
        }

        /**
         *
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
         *
         * @param elements
         * @return
         */
        public Builder<E> add(final E... elements) {
            if (N.notEmpty(elements)) {
                list.addAll(Arrays.asList(elements));
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
            if (N.notEmpty(c)) {
                list.addAll(c);
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
                    list.add(iter.next());
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
            return new ImmutableList<>(list);
        }
    }
}

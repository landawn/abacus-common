/*
 * Copyright (C) 2017 HaiYang Li
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

import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author Haiyang Li
 * @param <E>
 * @since 1.1.4
 */
public final class ImmutableNavigableSet<E> extends ImmutableSortedSet<E> implements NavigableSet<E> { //NOSONAR

    @SuppressWarnings("rawtypes")
    private static final ImmutableNavigableSet EMPTY = new ImmutableNavigableSet(N.emptyNavigableSet());

    private final NavigableSet<E> navigableSet;

    ImmutableNavigableSet(NavigableSet<? extends E> navigableSet) {
        super(navigableSet);
        this.navigableSet = (NavigableSet<E>) navigableSet;
    }

    /**
     *
     * @param <E>
     * @return
     */
    public static <E> ImmutableNavigableSet<E> empty() {
        return EMPTY;
    }

    /**
     *
     * @param <T>
     * @param e
     * @return
     */
    public static <T extends Comparable<? super T>> ImmutableNavigableSet<T> just(T e) {
        return new ImmutableNavigableSet<>(new TreeSet<>(N.asList(e)));
    }

    /**
     * 
     *
     * @param <T> 
     * @param e1 
     * @return 
     */
    public static <T extends Comparable<? super T>> ImmutableNavigableSet<T> of(final T e1) {
        return new ImmutableNavigableSet<>(new TreeSet<>(N.asList(e1)));
    }

    /**
     * 
     *
     * @param <T> 
     * @param e1 
     * @param e2 
     * @return 
     */
    public static <T extends Comparable<? super T>> ImmutableNavigableSet<T> of(final T e1, final T e2) {
        return new ImmutableNavigableSet<>(new TreeSet<>(N.asList(e1, e2)));
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
    public static <T extends Comparable<? super T>> ImmutableNavigableSet<T> of(final T e1, final T e2, final T e3) {
        return new ImmutableNavigableSet<>(new TreeSet<>(N.asList(e1, e2, e3)));
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
    public static <T extends Comparable<? super T>> ImmutableNavigableSet<T> of(final T e1, final T e2, final T e3, final T e4) {
        return new ImmutableNavigableSet<>(new TreeSet<>(N.asList(e1, e2, e3, e4)));
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
    public static <T extends Comparable<? super T>> ImmutableNavigableSet<T> of(final T e1, final T e2, final T e3, final T e4, final T e5) {
        return new ImmutableNavigableSet<>(new TreeSet<>(N.asList(e1, e2, e3, e4, e5)));
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
    public static <T extends Comparable<? super T>> ImmutableNavigableSet<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) {
        return new ImmutableNavigableSet<>(new TreeSet<>(N.asList(e1, e2, e3, e4, e5, e6)));
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
    public static <T extends Comparable<? super T>> ImmutableNavigableSet<T> of(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6,
            final T e7) {
        return new ImmutableNavigableSet<>(new TreeSet<>(N.asList(e1, e2, e3, e4, e5, e6, e7)));
    }

    /**
     *
     * @param <E>
     * @param sortedSet
     * @return
     */
    public static <E> ImmutableNavigableSet<E> copyOf(final SortedSet<? extends E> sortedSet) {
        if (N.isNullOrEmpty(sortedSet)) {
            return empty();
        }

        return new ImmutableNavigableSet<>(new TreeSet<>(sortedSet));
    }

    /**
     *
     * @param <E>
     * @param navigableSet
     * @return an {@code ImmutableNavigableSet} backed by the specified {@code navigableSet}
     * @deprecated the ImmutableNavigableSet may be modified through the specified {@code navigableSet}
     */
    @Deprecated
    public static <E> ImmutableNavigableSet<E> wrap(final NavigableSet<? extends E> navigableSet) {
        if (navigableSet == null) {
            return empty();
        } else if (navigableSet instanceof ImmutableNavigableSet) {
            return (ImmutableNavigableSet<E>) navigableSet;
        }

        return new ImmutableNavigableSet<>(navigableSet);
    }

    /**
     * 
     *
     * @param <E> 
     * @param sortedSet 
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    public static <E> ImmutableSortedSet<E> wrap(final SortedSet<? extends E> sortedSet) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param e
     * @return
     */
    @Override
    public E lower(E e) {
        return navigableSet.lower(e);
    }

    /**
     *
     * @param e
     * @return
     */
    @Override
    public E floor(E e) {
        return navigableSet.floor(e);
    }

    /**
     *
     * @param e
     * @return
     */
    @Override
    public E ceiling(E e) {
        return navigableSet.ceiling(e);
    }

    /**
     *
     * @param e
     * @return
     */
    @Override
    public E higher(E e) {
        return navigableSet.higher(e);
    }

    /**
     * 
     *
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated - UnsupportedOperationException
     */
    @Deprecated
    @Override
    public E pollFirst() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @return 
     * @throws UnsupportedOperationException 
     * @deprecated - UnsupportedOperationException
     */
    @Deprecated
    @Override
    public E pollLast() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public ImmutableNavigableSet<E> descendingSet() {
        return wrap(navigableSet.descendingSet());
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public ObjIterator<E> descendingIterator() {
        return ObjIterator.of(navigableSet.descendingIterator());
    }

    /**
     *
     * @param fromElement
     * @param fromInclusive
     * @param toElement
     * @param toInclusive
     * @return
     */
    @Override
    public ImmutableNavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return wrap(navigableSet.subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    /**
     *
     * @param toElement
     * @param inclusive
     * @return
     */
    @Override
    public ImmutableNavigableSet<E> headSet(E toElement, boolean inclusive) {
        return wrap(navigableSet.headSet(toElement, inclusive));
    }

    /**
     *
     * @param fromElement
     * @param inclusive
     * @return
     */
    @Override
    public ImmutableNavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return wrap(navigableSet.tailSet(fromElement, inclusive));
    }
}

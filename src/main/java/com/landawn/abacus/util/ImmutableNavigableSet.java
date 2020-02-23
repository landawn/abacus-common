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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The Class ImmutableNavigableSet.
 *
 * @author Haiyang Li
 * @param <E>
 * @since 1.1.4
 */
public final class ImmutableNavigableSet<E> extends ImmutableSortedSet<E> implements NavigableSet<E> {

    /** The Constant EMPTY. */
    @SuppressWarnings("rawtypes")
    private static final ImmutableNavigableSet EMPTY = new ImmutableNavigableSet(N.emptyNavigableSet());

    /** The navigable set. */
    private final NavigableSet<E> navigableSet;

    /**
     * Instantiates a new immutable navigable set.
     *
     * @param navigableSet
     */
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
     * @param <E>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(a)));
    }

    /**
     *
     * @param <E>
     * @param navigableSet the elements in this <code>Set</code> are shared by the returned ImmutableNavigableSet.
     * @return
     */
    public static <E> ImmutableNavigableSet<E> of(final NavigableSet<? extends E> navigableSet) {
        if (navigableSet == null) {
            return empty();
        } else if (navigableSet instanceof ImmutableNavigableSet) {
            return (ImmutableNavigableSet<E>) navigableSet;
        }

        return new ImmutableNavigableSet<>(navigableSet);
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
     * @param sortedSet
     * @return
     * @deprecated throws {@code UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    @Deprecated
    public static <E> ImmutableSortedSet<E> of(final SortedSet<? extends E> sortedSet) throws UnsupportedOperationException {
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
     * @return
     */
    @Override
    public E pollFirst() {
        return navigableSet.pollFirst();
    }

    /**
     *
     * @return
     */
    @Override
    public E pollLast() {
        return navigableSet.pollLast();
    }

    /**
     *
     * @return
     */
    @Override
    public NavigableSet<E> descendingSet() {
        return of(navigableSet.descendingSet());
    }

    /**
     *
     * @return
     */
    @Override
    public Iterator<E> descendingIterator() {
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
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return of(navigableSet.subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    /**
     *
     * @param toElement
     * @param inclusive
     * @return
     */
    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return of(navigableSet.headSet(toElement, inclusive));
    }

    /**
     *
     * @param fromElement
     * @param inclusive
     * @return
     */
    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return of(navigableSet.tailSet(fromElement, inclusive));
    }
}

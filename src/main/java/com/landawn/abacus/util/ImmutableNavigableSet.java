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

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableNavigableSet.
 *
 * @author Haiyang Li
 * @param <E> the element type
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
     * @param navigableSet the navigable set
     */
    ImmutableNavigableSet(NavigableSet<? extends E> navigableSet) {
        super(navigableSet);
        this.navigableSet = (NavigableSet<E>) navigableSet;
    }

    /**
     * Empty.
     *
     * @param <E> the element type
     * @return the immutable navigable set
     */
    public static <E> ImmutableNavigableSet<E> empty() {
        return EMPTY;
    }

    /**
     * Of.
     *
     * @param <E> the element type
     * @param a the a
     * @return the immutable navigable set
     */
    @SafeVarargs
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(a)));
    }

    /**
     * Of.
     *
     * @param <E> the element type
     * @param navigableSet the elements in this <code>Set</code> are shared by the returned ImmutableNavigableSet.
     * @return the immutable navigable set
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
     * Copy of.
     *
     * @param <E> the element type
     * @param sortedSet the sorted set
     * @return the immutable navigable set
     */
    public static <E> ImmutableNavigableSet<E> copyOf(final SortedSet<? extends E> sortedSet) {
        if (N.isNullOrEmpty(sortedSet)) {
            return empty();
        }

        return new ImmutableNavigableSet<>(new TreeSet<>(sortedSet));
    }

    /**
     * Of.
     *
     * @param <E> the element type
     * @param sortedSet the sorted set
     * @return the immutable sorted set
     */
    @Deprecated
    public static <E> ImmutableSortedSet<E> of(final SortedSet<? extends E> sortedSet) {
        throw new UnsupportedOperationException();
    }

    /**
     * Lower.
     *
     * @param e the e
     * @return the e
     */
    @Override
    public E lower(E e) {
        return navigableSet.lower(e);
    }

    /**
     * Floor.
     *
     * @param e the e
     * @return the e
     */
    @Override
    public E floor(E e) {
        return navigableSet.floor(e);
    }

    /**
     * Ceiling.
     *
     * @param e the e
     * @return the e
     */
    @Override
    public E ceiling(E e) {
        return navigableSet.ceiling(e);
    }

    /**
     * Higher.
     *
     * @param e the e
     * @return the e
     */
    @Override
    public E higher(E e) {
        return navigableSet.higher(e);
    }

    /**
     * Poll first.
     *
     * @return the e
     */
    @Override
    public E pollFirst() {
        return navigableSet.pollFirst();
    }

    /**
     * Poll last.
     *
     * @return the e
     */
    @Override
    public E pollLast() {
        return navigableSet.pollLast();
    }

    /**
     * Descending set.
     *
     * @return the navigable set
     */
    @Override
    public NavigableSet<E> descendingSet() {
        return of(navigableSet.descendingSet());
    }

    /**
     * Descending iterator.
     *
     * @return the iterator
     */
    @Override
    public Iterator<E> descendingIterator() {
        return ObjIterator.of(navigableSet.descendingIterator());
    }

    /**
     * Sub set.
     *
     * @param fromElement the from element
     * @param fromInclusive the from inclusive
     * @param toElement the to element
     * @param toInclusive the to inclusive
     * @return the navigable set
     */
    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return of(navigableSet.subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    /**
     * Head set.
     *
     * @param toElement the to element
     * @param inclusive the inclusive
     * @return the navigable set
     */
    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return of(navigableSet.headSet(toElement, inclusive));
    }

    /**
     * Tail set.
     *
     * @param fromElement the from element
     * @param inclusive the inclusive
     * @return the navigable set
     */
    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return of(navigableSet.tailSet(fromElement, inclusive));
    }
}

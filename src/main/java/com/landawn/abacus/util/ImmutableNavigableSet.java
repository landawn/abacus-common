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
import java.util.Collection;
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

    ImmutableNavigableSet(final NavigableSet<? extends E> navigableSet) {
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
     * @param e
     * @return
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> just(final E e) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e)));
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @return
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1)));
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @return
     */
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2)));
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
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3)));
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
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4)));
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
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5)));
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
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6)));
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
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6,
            final E e7) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7)));
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
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6,
            final E e7, final E e8) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8)));
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
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6,
            final E e7, final E e8, final E e9) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9)));
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
    public static <E extends Comparable<? super E>> ImmutableNavigableSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6,
            final E e7, final E e8, final E e9, final E e10) {
        return new ImmutableNavigableSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10)));
    }

    /**
     *
     *
     * @param <E>
     * @param c
     * @return
     */
    public static <E> ImmutableNavigableSet<E> copyOf(final Collection<? extends E> c) {
        if (c instanceof ImmutableNavigableSet) {
            return (ImmutableNavigableSet<E>) c;
        } else if (N.isEmpty(c)) {
            return empty();
        } else {
            return new ImmutableNavigableSet<>(new TreeSet<>(c));
        }
    }

    /**
     *
     * @param <E>
     * @param navigableSet
     * @return an {@code ImmutableNavigableSet} backed by the specified {@code sortedSet}
     */
    public static <E> ImmutableNavigableSet<E> wrap(final NavigableSet<? extends E> navigableSet) {
        if (navigableSet instanceof ImmutableNavigableSet) {
            return (ImmutableNavigableSet<E>) navigableSet;
        } else if (navigableSet == null) {
            return empty();
        } else {
            return new ImmutableNavigableSet<>(navigableSet);
        }
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
    public E lower(final E e) {
        return navigableSet.lower(e);
    }

    /**
     *
     * @param e
     * @return
     */
    @Override
    public E floor(final E e) {
        return navigableSet.floor(e);
    }

    /**
     *
     * @param e
     * @return
     */
    @Override
    public E ceiling(final E e) {
        return navigableSet.ceiling(e);
    }

    /**
     *
     * @param e
     * @return
     */
    @Override
    public E higher(final E e) {
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
    public ImmutableNavigableSet<E> subSet(final E fromElement, final boolean fromInclusive, final E toElement, final boolean toInclusive) {
        return wrap(navigableSet.subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    /**
     *
     * @param toElement
     * @param inclusive
     * @return
     */
    @Override
    public ImmutableNavigableSet<E> headSet(final E toElement, final boolean inclusive) {
        return wrap(navigableSet.headSet(toElement, inclusive));
    }

    /**
     *
     * @param fromElement
     * @param inclusive
     * @return
     */
    @Override
    public ImmutableNavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
        return wrap(navigableSet.tailSet(fromElement, inclusive));
    }
}

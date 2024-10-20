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
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.landawn.abacus.annotation.Beta;

/**
 *
 * @param <E>
 */
@SuppressWarnings("java:S2160")
public class ImmutableSortedSet<E> extends ImmutableSet<E> implements SortedSet<E> {

    @SuppressWarnings("rawtypes")
    private static final ImmutableSortedSet EMPTY = new ImmutableSortedSet(N.emptySortedSet());

    private final SortedSet<E> sortedSet;

    ImmutableSortedSet(final SortedSet<? extends E> sortedSet) {
        super(sortedSet);
        this.sortedSet = (SortedSet<E>) sortedSet;
    }

    /**
     *
     * @param <E>
     * @return
     */
    public static <E> ImmutableSortedSet<E> empty() {
        return EMPTY;
    }

    /**
     *
     * @param <E>
     * @param e
     * @return
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> just(final E e) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e)));
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @return
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1)));
    }

    /**
     *
     *
     * @param <E>
     * @param e1
     * @param e2
     * @return
     */
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2)));
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
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3)));
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
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4)));
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
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5)));
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
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6)));
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
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6,
            final E e7) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7)));
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
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7,
            final E e8) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8)));
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
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7,
            final E e8, final E e9) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9)));
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
    public static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(final E e1, final E e2, final E e3, final E e4, final E e5, final E e6, final E e7,
            final E e8, final E e9, final E e10) {
        return new ImmutableSortedSet<>(new TreeSet<>(Arrays.asList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10)));
    }

    /**
     * Returns an ImmutableSortedSet containing the elements of the specified collection.
     * If the provided collection is already an instance of ImmutableSortedSet, it is directly returned.
     * If the provided collection is {@code null} or empty, an empty ImmutableSortedSet is returned.
     * Otherwise, a new ImmutableSortedSet is created with the elements of the provided collection.
     *
     * @param <E> the type of elements in the collection
     * @param c the collection whose elements are to be placed into this set
     * @return an ImmutableSortedSet containing the elements of the specified collection
     */
    public static <E> ImmutableSortedSet<E> copyOf(final Collection<? extends E> c) {
        if (c instanceof ImmutableSortedSet) {
            return (ImmutableSortedSet<E>) c;
        } else if (N.isEmpty(c)) {
            return empty();
        } else {
            return new ImmutableSortedSet<>(new TreeSet<>(c));
        }
    }

    /**
     * Wraps the provided SortedSet into an ImmutableSortedSet. Changes to the specified SortedSet will be reflected in the ImmutableSortedSet.
     * If the provided SortedSet is already an instance of ImmutableSortedSet, it is directly returned.
     * If the provided SortedSet is {@code null}, an empty ImmutableSortedSet is returned.
     * Otherwise, returns a new ImmutableSortedSet backed by the provided SortedSet.
     *
     * @param <E>
     * @param sortedSet
     * @return an {@code ImmutableSortedSet} backed by the specified {@code sortedSet}
     */
    @Beta
    public static <E> ImmutableSortedSet<E> wrap(final SortedSet<? extends E> sortedSet) {
        if (sortedSet instanceof ImmutableSortedSet) {
            return (ImmutableSortedSet<E>) sortedSet;
        } else if (sortedSet == null) {
            return empty();
        } else {
            return new ImmutableSortedSet<>(sortedSet);
        }
    }

    /**
     * This method is deprecated and will throw an UnsupportedOperationException if used.
     *
     * @param <E>
     * @param set
     * @return
     * @throws UnsupportedOperationException
     * @deprecated throws {@code UnsupportedOperationException}
     */
    @Deprecated
    public static <E> ImmutableSet<E> wrap(final Set<? extends E> set) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Comparator<? super E> comparator() {
        return sortedSet.comparator();
    }

    /**
     *
     * @param fromElement
     * @param toElement
     * @return
     */
    @Override
    public ImmutableSortedSet<E> subSet(final E fromElement, final E toElement) {
        return wrap(sortedSet.subSet(fromElement, toElement));
    }

    /**
     *
     * @param toElement
     * @return
     */
    @Override
    public ImmutableSortedSet<E> headSet(final E toElement) {
        return wrap(sortedSet.headSet(toElement));
    }

    /**
     *
     * @param fromElement
     * @return
     */
    @Override
    public ImmutableSortedSet<E> tailSet(final E fromElement) {
        return wrap(sortedSet.tailSet(fromElement));
    }

    /**
     *
     *
     * @return
     */
    @Override
    public E first() {
        return sortedSet.first();
    }

    /**
     *
     *
     * @return
     */
    @Override
    public E last() {
        return sortedSet.last();
    }
}
